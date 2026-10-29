package com.eema.backend.mapper;

import com.eema.backend.dto.request.CreateVenueRequest;
import com.eema.backend.dto.request.UpdateVenueRequest;
import com.eema.backend.dto.response.VenueDto;
import com.eema.backend.dto.response.VenueListDto;
import com.eema.backend.entity.Venue;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Mapper for converting between Venue entity and DTOs.
 */
@Component
public class VenueMapper {

    private final UserMapper userMapper;

    public VenueMapper(UserMapper userMapper) {
        this.userMapper = userMapper;
    }


    /**
     * Convert Venue entity to VenueDto.
     */
    public VenueDto toDto(Venue venue) {

        if (venue == null) {
            return null;
        }

        return new VenueDto(
                venue.getId(),
                userMapper.toDto(venue.getOwner()),
                venue.getName(),
                venue.getNeighbourhood(),
                venue.getSizeM2(),
                venue.getHourlyRateEur(),
                venue.getMinimumBookingMinutes(),
                venue.getFullDayRateEur(),
                venue.getMaxCrewSize(),
                nullableList(venue.getPermittedActivities()),
                nullableList(venue.getProhibitedActivities()),
                venue.getNaturalLight(),
                venue.getBlackoutPossible(),
                venue.getCeilingHeightM(),
                venue.getLoadingAccess(),
                venue.getParkingAvailable(),
                venue.getParkingSpaces(),
                venue.getFloorNumber(),
                venue.getLiftAvailable(),
                venue.getWheelchairAccessible(),
                venue.getBathroomsAvailable(),
                venue.getSoundRecordingSuitable(),
                venue.getProductionRules(),
                venue.getPowerDetails(),
                safeList(venue.getTags()),
                safeList(venue.getPhotoPaths()),
                venue.getClipUrl(),
                venue.getDescription(),
                venue.getTimezone(),
                venue.getStatus(),
                venue.getCreatedAt(),
                venue.getUpdatedAt()
        );
    }


    /**
     * Convert Venue entity to list response DTO.
     */
    public VenueListDto toListDto(Venue venue) {

        if (venue == null) {
            return null;
        }

        String firstPhotoUrl =
                safeList(venue.getPhotoPaths())
                        .stream()
                        .findFirst()
                        .orElse(null);


        return new VenueListDto(
                venue.getId(),
                venue.getName(),
                venue.getNeighbourhood(),
                venue.getSizeM2(),
                venue.getHourlyRateEur(),
                venue.getMinimumBookingMinutes(),
                venue.getFullDayRateEur(),
                venue.getMaxCrewSize(),
                venue.getNaturalLight(),
                venue.getLoadingAccess(),
                venue.getParkingAvailable(),
                venue.getSoundRecordingSuitable(),
                safeList(venue.getTags()),
                firstPhotoUrl,
                venue.getStatus()
        );
    }


    /**
     * Convert CreateVenueRequest to Venue entity.
     */
    public Venue toEntity(CreateVenueRequest request) {

        if (request == null) {
            return null;
        }


        return Venue.builder()
                .name(request.name())
                .neighbourhood(request.neighbourhood())
                .sizeM2(request.sizeM2())
                .hourlyRateEur(request.hourlyRateEur())
                .minimumBookingMinutes(request.minimumBookingMinutes())
                .fullDayRateEur(request.fullDayRateEur())
                .maxCrewSize(request.maxCrewSize())
                .permittedActivities(nullableList(request.permittedActivities()))
                .prohibitedActivities(nullableList(request.prohibitedActivities()))
                .naturalLight(request.naturalLight())
                .blackoutPossible(request.blackoutPossible())
                .ceilingHeightM(request.ceilingHeightM())
                .loadingAccess(request.loadingAccess())
                .parkingAvailable(request.parkingAvailable())
                .parkingSpaces(request.parkingSpaces())
                .floorNumber(request.floorNumber())
                .liftAvailable(request.liftAvailable())
                .wheelchairAccessible(request.wheelchairAccessible())
                .bathroomsAvailable(request.bathroomsAvailable())
                .soundRecordingSuitable(request.soundRecordingSuitable())
                .productionRules(request.productionRules())
                .powerDetails(request.powerDetails())
                .tags(
                        request.tags() == null
                                ? List.of()
                                : request.tags()
                )
                .description(request.description())
                .clipUrl(request.clipUrl())
                .build();
    }


    /**
     * Update existing Venue entity.
     */
    public void updateEntityFromDto(
            UpdateVenueRequest request,
            Venue venue
    ) {

        if (request == null || venue == null) {
            return;
        }


        venue.setName(request.name());
        venue.setNeighbourhood(request.neighbourhood());
        venue.setSizeM2(request.sizeM2());
        venue.setHourlyRateEur(request.hourlyRateEur());
        venue.setMinimumBookingMinutes(request.minimumBookingMinutes());
        venue.setFullDayRateEur(request.fullDayRateEur());
        venue.setMaxCrewSize(request.maxCrewSize());
        venue.setPermittedActivities(nullableList(request.permittedActivities()));
        venue.setProhibitedActivities(nullableList(request.prohibitedActivities()));
        venue.setNaturalLight(request.naturalLight());
        venue.setBlackoutPossible(request.blackoutPossible());
        venue.setCeilingHeightM(request.ceilingHeightM());
        venue.setLoadingAccess(request.loadingAccess());
        venue.setParkingAvailable(request.parkingAvailable());
        venue.setParkingSpaces(request.parkingSpaces());
        venue.setFloorNumber(request.floorNumber());
        venue.setLiftAvailable(request.liftAvailable());
        venue.setWheelchairAccessible(request.wheelchairAccessible());
        venue.setBathroomsAvailable(request.bathroomsAvailable());
        venue.setSoundRecordingSuitable(request.soundRecordingSuitable());
        venue.setProductionRules(request.productionRules());
        venue.setPowerDetails(request.powerDetails());

        venue.setTags(
                request.tags() == null
                        ? List.of()
                        : request.tags()
        );

        venue.setDescription(request.description());
        venue.setClipUrl(request.clipUrl());
        venue.setTimezone(request.timezone());
        venue.setPhotoPaths(List.copyOf(request.photoPaths()));
    }


    private List<String> safeList(List<String> values) {
        return values == null
                ? List.of()
                : List.copyOf(values);
    }

    private <T> List<T> nullableList(List<T> values) {
        return values == null ? null : List.copyOf(values);
    }

}
