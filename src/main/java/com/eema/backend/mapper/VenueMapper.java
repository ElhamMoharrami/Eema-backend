package com.eema.backend.mapper;

import com.eema.backend.dto.request.CreateVenueRequest;
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
                venue.getDayRateEur(),
                safeList(venue.getTags()),
                safeList(venue.getPhotoPaths()),
                venue.getClipUrl(),
                venue.getDescription(),
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
                venue.getDayRateEur(),
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
                .dayRateEur(request.dayRateEur())
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
            CreateVenueRequest request,
            Venue venue
    ) {

        if (request == null || venue == null) {
            return;
        }


        venue.setName(request.name());
        venue.setNeighbourhood(request.neighbourhood());
        venue.setSizeM2(request.sizeM2());
        venue.setDayRateEur(request.dayRateEur());

        venue.setTags(
                request.tags() == null
                        ? List.of()
                        : request.tags()
        );

        venue.setDescription(request.description());
        venue.setClipUrl(request.clipUrl());
    }


    private List<String> safeList(List<String> values) {
        return values == null
                ? List.of()
                : List.copyOf(values);
    }
}