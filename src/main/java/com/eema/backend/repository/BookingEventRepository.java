package com.eema.backend.repository;

import com.eema.backend.entity.BookingEvent;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BookingEventRepository
        extends JpaRepository<BookingEvent, UUID> {

    List<BookingEvent> findByBookingIdOrderByCreatedAtAsc(
            UUID bookingId
    );

    void deleteAllByBookingId(UUID bookingId);
}