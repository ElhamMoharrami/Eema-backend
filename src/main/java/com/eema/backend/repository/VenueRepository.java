package com.eema.backend.repository;

import com.eema.backend.entity.Venue;
import com.eema.backend.entity.enums.VenueStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Spring Data JPA repository for the Venue entity.
 */
@Repository
public interface VenueRepository extends JpaRepository<Venue, UUID> {

    List<Venue> findByStatus(VenueStatus status);

    @Query(value = """
    SELECT *
    FROM venues v
    WHERE v.status = CAST(:status AS venue_status)
    AND :tag = ANY(v.tags)
    """, nativeQuery = true)
    List<Venue> findByStatusAndTagsContaining(
            @Param("status") String status,
            @Param("tag") String tag
    );

    List<Venue> findByOwnerId(UUID ownerId);

    Page<Venue> findByStatus(VenueStatus status, Pageable pageable);
}
