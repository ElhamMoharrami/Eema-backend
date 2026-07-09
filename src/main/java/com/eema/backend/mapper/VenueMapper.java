package com.eema.backend.mapper;

import com.eema.backend.dto.request.CreateVenueRequest;
import com.eema.backend.dto.response.VenueDto;
import com.eema.backend.dto.response.VenueListDto;
import com.eema.backend.entity.User;
import com.eema.backend.entity.Venue;
import com.eema.backend.entity.VenueStatus;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Mapper for converting between Venue entity and VenueDto.
 */
@Component
public class VenueMapper {

    /**
     * Convert Venue entity to VenueDto.
     *
     * @param venue the venue entity
     * @return venue DTO
     */
    public VenueDto toDto(Venue venue) {
        return new VenueDto(
                venue.getId(),
                venue.getOwner(),
                venue.getName(),
                venue.getNeighbourhood(),
                venue.getSizeM2(),
                venue.getDayRateEur(),
                venue.getTags(),
                venue.getPhotoPaths(),
                venue.getClipUrl(),
                venue.getDescription(),
                venue.getStatus(),
                venue.getCreatedAt(),
                venue.getUpdatedAt()
        );
    }

    /**
     * Convert Venue entity to VenueListDto.
     *
     * @param venue the venue entity
     * @return venue list DTO
     */
    public VenueListDto toListDto(Venue venue) {
        String firstPhotoUrl = null;
        if (venue.getPhotoPaths() != null && !venue.getPhotoPaths().isEmpty()) {
            firstPhotoUrl = venue.getPhotoPaths().get(0);
        }
        return new VenueListDto(
                venue.getId(),
                venue.getName(),
                venue.getNeighbourhood(),
                venue.getSizeM2(),
                venue.getDayRateEur(),
                venue.getTags(),
                firstPhotoUrl,
                venue.getStatus()
        );
    }

    /**
     * Convert CreateVenueRequest to Venue entity.
     *
     * @param request the venue creation request
     * @return venue entity
     */
    public Venue toEntity(CreateVenueRequest request) {
        return Venue.builder()
                .name(request.name())
                .neighbourhood(request.neighbourhood())
                .sizeM2(request.sizeM2())
                .dayRateEur(request.dayRateEur())
                .tags(request.tags())
                .description(request.description())
                .clipUrl(request.clipUrl())
                .status(VenueStatus.ACTIVE) // Default status
                .build();
    }

    /**
     * Update Venue entity with data from CreateVenueRequest.
     *
     * @param request the venue update request
     * @param venue   the venue entity to update
     */
    public void updateEntityFromDto(CreateVenueRequest request, Venue venue) {
        venue.setName(request.name());
        venue.setNeighbourhood(request.neighbourhood());
        venue.setSizeM2(request.sizeM2());
        venue.setDayRateEur(request.dayRateEur());
        venue.setTags(request.tags());
        venue.setDescription(request.description());
        venue.setClipUrl(request.clipUrl());
        // Note: status, owner, createdAt, updatedAt are not updated via this method
    }
}