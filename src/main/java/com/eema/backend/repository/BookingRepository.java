package com.eema.backend.repository;

import com.eema.backend.entity.Booking;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BookingRepository extends JpaRepository<Booking, UUID> {

    List<Booking> findByScoutId(UUID scoutId);

    List<Booking> findByVenueIdIn(List<UUID> venueIds);

    Optional<Booking> findByMolliePaymentId(String molliePaymentId);

    boolean existsByMolliePaymentId(String molliePaymentId);
}