package com.eema.backend.service;

import com.eema.backend.entity.Venue;
import com.eema.backend.service.ai.ParsedBrief;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.DateTimeException;
import java.time.ZoneId;
import java.util.List;

@Service
@RequiredArgsConstructor
public class VenueMatchAvailabilityFilter {

    private final VenueAvailabilityService availabilityService;

    public List<Venue> retainAvailable(List<Venue> candidates, ParsedBrief brief) {
        if (!hasExactInterval(brief)) {
            return candidates;
        }

        return candidates.stream()
                .filter(venue -> isAvailableForEntireInterval(venue, brief))
                .toList();
    }

    public boolean hasExactInterval(ParsedBrief brief) {
        return brief.requestedDate() != null
                && brief.requestedStartTime() != null
                && brief.requestedEndTime() != null;
    }

    private boolean isAvailableForEntireInterval(Venue venue, ParsedBrief brief) {
        requireValidVenueTimezone(venue);
        return availabilityService.getAvailableIntervals(venue.getId(), brief.requestedDate())
                .stream()
                .anyMatch(interval -> !brief.requestedStartTime().isBefore(interval.startTime())
                        && !brief.requestedEndTime().isAfter(interval.endTime()));
    }

    private void requireValidVenueTimezone(Venue venue) {
        try {
            ZoneId.of(venue.getTimezone());
        } catch (DateTimeException | NullPointerException exception) {
            throw new IllegalStateException(
                    "Venue has an invalid timezone: " + venue.getId(),
                    exception
            );
        }
    }
}
