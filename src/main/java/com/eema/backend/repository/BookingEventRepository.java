package com.eema.backend.repository;

import com.eema.backend.entity.BookingEvent;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@Repository
public interface BookingEventRepository
        extends JpaRepository<BookingEvent, UUID> {

    List<BookingEvent> findByBookingIdOrderByCreatedAtAsc(
            UUID bookingId
    );
    Page<BookingEvent> findPageByBookingIdOrderByCreatedAtAsc(UUID bookingId, Pageable pageable);

    void deleteAllByBookingId(UUID bookingId);
}
