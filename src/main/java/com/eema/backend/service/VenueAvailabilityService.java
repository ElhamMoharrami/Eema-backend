package com.eema.backend.service;

import com.eema.backend.dto.request.*;
import com.eema.backend.dto.response.*;
import com.eema.backend.entity.*;
import com.eema.backend.entity.enums.AvailabilityOverrideType;
import com.eema.backend.entity.enums.UserRole;
import com.eema.backend.exception.BadRequestException;
import com.eema.backend.exception.ForbiddenException;
import com.eema.backend.exception.NotFoundException;
import com.eema.backend.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
@RequiredArgsConstructor
public class VenueAvailabilityService {
    public static final long MAX_RANGE_DAYS = 93;
    private final VenueRepository venues;
    private final VenueOperatingHoursRepository weeklyRepository;
    private final VenueAvailabilityOverrideRepository overrideRepository;
    private final CurrentUserService currentUserService;
    private final VenueService venueService;
    private final BookingRepository bookingRepository;
    private final VenueBookingLeadTimePolicy leadTimePolicy;

    @Transactional(readOnly = true)
    public VenueAvailabilityDto getAvailability(UUID venueId, LocalDate from, LocalDate to) {
        validateRange(from, to);
        venueService.getVenueById(venueId); // applies existing public/non-active visibility policy
        Venue venue = findVenue(venueId);
        List<DailyAvailabilityDto> dates = from.datesUntil(to.plusDays(1))
                .map(date -> new DailyAvailabilityDto(date, getAvailableIntervals(venueId, date))).toList();
        return new VenueAvailabilityDto(venueId, venue.getTimezone(), dates);
    }

    @Transactional(readOnly = true)
    public List<AvailabilityIntervalDto> getAvailableIntervals(UUID venueId, LocalDate date) {
        return getAvailableIntervalsExcludingBooking(venueId, date, null);
    }

    @Transactional(readOnly = true)
    public List<AvailabilityIntervalDto> getAvailableIntervalsExcludingBooking(UUID venueId, LocalDate date, UUID excludedBookingId) {
        Venue venue = findVenue(venueId);
        List<Interval> available = weeklyRepository.findByVenueIdAndDayOfWeekOrderByStartTime(venueId, date.getDayOfWeek())
                .stream().map(row -> new Interval(row.getStartTime(), row.getEndTime())).collect(java.util.stream.Collectors.toCollection(ArrayList::new));
        List<Interval> blocked = new ArrayList<>();
        for (VenueAvailabilityOverride override : overrideRepository.findByVenueIdAndDateOrderByStartTime(venueId, date)) {
            Interval interval = override.isAllDay() ? new Interval(LocalTime.MIN, LocalTime.MAX)
                    : new Interval(override.getStartTime(), override.getEndTime());
            if (override.getType() == AvailabilityOverrideType.AVAILABLE) available.add(interval); else blocked.add(interval);
        }
        bookingRepository.findBlockingBookings(venueId, date).stream()
                .filter(booking -> excludedBookingId == null || !excludedBookingId.equals(booking.getId()))
                .filter(booking -> BookingReservationPolicy.blocksAvailability(booking.getStatus()))
                .forEach(booking -> blocked.add(new Interval(booking.getStartTime(), booking.getEndTime())));
        List<Interval> result = normalize(available);
        for (Interval block : normalize(blocked)) result = subtract(result, block);
        result = applyLeadTime(result, venue, date);
        return normalize(result).stream().flatMap(i -> BookingTimePolicy.available(date, i.start(), i.end(),
                ZoneId.of(venue.getTimezone())).stream()).toList();
    }

    private List<Interval> applyLeadTime(List<Interval> intervals, Venue venue, LocalDate date) {
        ZoneId zone = ZoneId.of(venue.getTimezone());
        Instant earliestInstant = leadTimePolicy.earliestStart(venue);
        Instant rounded = earliestInstant.truncatedTo(ChronoUnit.MINUTES);
        if (rounded.isBefore(earliestInstant)) rounded = rounded.plus(1, ChronoUnit.MINUTES);
        ZonedDateTime threshold = rounded.atZone(zone);
        // Requests resolve an ambiguous local time to its earlier offset. The second occurrence
        // therefore cannot be selected: move beyond the overlap, without inventing offset input.
        if (threshold.withEarlierOffsetAtOverlap().toInstant().isBefore(rounded)) {
            var afterOverlap = zone.getRules().getTransition(threshold.toLocalDateTime()).getDateTimeBefore();
            threshold = BookingTimePolicy.resolve(afterOverlap.toLocalDate(), afterOverlap.toLocalTime(), zone).toZonedDateTime();
        }
        // Compare dates after rounding so 23:59:xx cannot wrap to 00:00 on the same day.
        if (date.isBefore(threshold.toLocalDate())) return List.of();
        if (date.isAfter(threshold.toLocalDate())) return intervals;
        LocalTime clip = threshold.toLocalTime();
        return intervals.stream().filter(interval -> interval.end().isAfter(clip))
                .map(interval -> new Interval(interval.start().isBefore(clip) ? clip : interval.start(), interval.end()))
                .toList();
    }

    @Transactional(readOnly = true)
    public VenueAvailabilitySettingsDto getSettings(UUID venueId) {
        Venue venue = requireMutableVenue(venueId, false);
        return settings(venue);
    }

    @Transactional
    public VenueAvailabilitySettingsDto updateSettings(UUID venueId, UpdateVenueAvailabilitySettingsRequest request) {
        Venue venue = requireMutableVenue(venueId, true);
        try { ZoneId.of(request.timezone()); }
        catch (DateTimeException exception) { throw new BadRequestException("timezone must be a valid IANA ZoneId"); }
        venue.setTimezone(request.timezone());
        return settings(venue);
    }

    @Transactional
    public VenueAvailabilitySettingsDto replaceWeekly(UUID venueId, UpdateWeeklyAvailabilityRequest request) {
        Venue venue = requireMutableVenue(venueId, true);
        validateWeekly(request.intervals());
        weeklyRepository.deleteByVenueId(venueId);
        weeklyRepository.flush();
        weeklyRepository.saveAll(request.intervals().stream().map(i -> VenueOperatingHours.builder()
                .venue(venue).dayOfWeek(i.dayOfWeek()).startTime(i.startTime()).endTime(i.endTime()).build()).toList());
        return settings(venue);
    }

    @Transactional
    public AvailabilityOverrideDto addOverride(UUID venueId, CreateAvailabilityOverrideRequest request) {
        Venue venue = requireMutableVenue(venueId, true);
        validateOverride(request);
        VenueAvailabilityOverride saved = overrideRepository.save(VenueAvailabilityOverride.builder().venue(venue)
                .date(request.date()).startTime(request.startTime()).endTime(request.endTime()).type(request.type())
                .allDay(request.allDay()).build());
        return overrideDto(saved);
    }

    @Transactional
    public void deleteOverride(UUID venueId, UUID overrideId) {
        requireMutableVenue(venueId, true);
        VenueAvailabilityOverride override = overrideRepository.findById(overrideId)
                .orElseThrow(() -> new NotFoundException("Availability override not found"));
        if (!override.getVenue().getId().equals(venueId)) throw new NotFoundException("Availability override not found");
        overrideRepository.delete(override);
    }

    private Venue requireMutableVenue(UUID venueId, boolean lock) {
        Venue venue = (lock ? venues.findByIdForUpdate(venueId) : venues.findById(venueId))
                .orElseThrow(() -> new NotFoundException("Venue not found"));
        User user = currentUserService.getCurrentUser();
        boolean owner = user.getRole() == UserRole.OWNER && venue.getOwner() != null && venue.getOwner().getId().equals(user.getId());
        if (user.getRole() != UserRole.ADMIN && !owner) throw new ForbiddenException("Only the venue owner or admin can manage availability");
        return venue;
    }

    private Venue findVenue(UUID id) { return venues.findById(id).orElseThrow(() -> new NotFoundException("Venue not found")); }

    private void validateRange(LocalDate from, LocalDate to) {
        if (from == null || to == null || to.isBefore(from)) throw new BadRequestException("from must be on or before to");
        if (ChronoUnit.DAYS.between(from, to) + 1 > MAX_RANGE_DAYS) throw new BadRequestException("Date range may not exceed " + MAX_RANGE_DAYS + " days");
    }

    private void validateWeekly(List<WeeklyIntervalRequest> intervals) {
        for (WeeklyIntervalRequest interval : intervals) requireValid(interval.startTime(), interval.endTime());
        Map<DayOfWeek, List<WeeklyIntervalRequest>> byDay = new EnumMap<>(DayOfWeek.class);
        intervals.forEach(i -> byDay.computeIfAbsent(i.dayOfWeek(), ignored -> new ArrayList<>()).add(i));
        byDay.values().forEach(day -> {
            day.sort(Comparator.comparing(WeeklyIntervalRequest::startTime));
            for (int i = 1; i < day.size(); i++) if (day.get(i).startTime().isBefore(day.get(i - 1).endTime()))
                throw new BadRequestException("Weekly intervals may not overlap on the same day");
        });
    }

    private void validateOverride(CreateAvailabilityOverrideRequest request) {
        if (request.allDay()) {
            if (request.startTime() != null || request.endTime() != null) throw new BadRequestException("All-day overrides must not specify times");
        } else {
            requireValid(request.startTime(), request.endTime());
        }
    }

    private void requireValid(LocalTime start, LocalTime end) {
        BookingOverlapRule.requireValid(start, end);
    }

    private VenueAvailabilitySettingsDto settings(Venue venue) {
        List<WeeklyIntervalDto> weekly = weeklyRepository.findByVenueIdOrderByDayOfWeekAscStartTimeAsc(venue.getId()).stream()
                .map(i -> new WeeklyIntervalDto(i.getId(), i.getDayOfWeek(), i.getStartTime(), i.getEndTime())).toList();
        List<AvailabilityOverrideDto> overrides = overrideRepository.findByVenueIdOrderByDateAscStartTimeAsc(venue.getId()).stream().map(this::overrideDto).toList();
        return new VenueAvailabilitySettingsDto(venue.getId(), venue.getTimezone(), weekly, overrides);
    }

    private AvailabilityOverrideDto overrideDto(VenueAvailabilityOverride value) {
        return new AvailabilityOverrideDto(value.getId(), value.getDate(), value.getStartTime(), value.getEndTime(), value.getType(), value.isAllDay());
    }

    private List<Interval> normalize(List<Interval> values) {
        if (values.isEmpty()) return List.of();
        List<Interval> sorted = values.stream().sorted(Comparator.comparing(Interval::start)).toList();
        List<Interval> result = new ArrayList<>();
        for (Interval next : sorted) {
            if (result.isEmpty() || next.start().isAfter(result.getLast().end())) result.add(next);
            else {
                Interval previous = result.removeLast();
                result.add(new Interval(previous.start(), previous.end().isAfter(next.end()) ? previous.end() : next.end()));
            }
        }
        return result;
    }

    private List<Interval> subtract(List<Interval> source, Interval block) {
        List<Interval> result = new ArrayList<>();
        for (Interval value : source) {
            if (!BookingOverlapRule.overlaps(block.start(), block.end(), value.start(), value.end())) { result.add(value); continue; }
            if (block.start().isAfter(value.start())) result.add(new Interval(value.start(), block.start()));
            if (block.end().isBefore(value.end())) result.add(new Interval(block.end(), value.end()));
        }
        return result;
    }

    private record Interval(LocalTime start, LocalTime end) {}
}
