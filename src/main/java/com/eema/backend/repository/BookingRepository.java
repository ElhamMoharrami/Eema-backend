package com.eema.backend.repository;

import com.eema.backend.entity.Booking;
import com.eema.backend.entity.enums.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BookingRepository extends JpaRepository<Booking, UUID> {

    List<Booking> findByScoutIdOrderByCreatedAtDesc(UUID scoutId);

    List<Booking> findByVenueIdIn(List<UUID> venueIds);

    List<Booking> findByStatus(BookingStatus status);

    Optional<Booking> findByMolliePaymentId(String molliePaymentId);

    boolean existsByMolliePaymentId(String molliePaymentId);
}