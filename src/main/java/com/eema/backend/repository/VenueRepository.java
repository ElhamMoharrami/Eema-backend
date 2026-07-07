package com.eema.backend.repository;

import com.eema.backend.entity.Venue;
import com.eema.backend.entity.VenueStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@Repository
public interface VenueRepository extends JpaRepository<Venue, UUID> {

    List<Venue> findByStatus(VenueStatus status);

    @Query(value = "SELECT * FROM venues WHERE status = :status AND :tag = ANY(tags)", nativeQuery = true)
    List<Venue> findByStatusAndTagsContaining(@Param("status") VenueStatus status, @Param("tag") String tag);

    Optional<Venue> findByIdAndOwnerId(UUID id, UUID ownerId);

    List<Venue> findByOwnerId(UUID ownerId);

    Page<Venue> findByStatus(VenueStatus status, Pageable pageable);
}