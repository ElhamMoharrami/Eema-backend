package com.eema.backend.repository;
import com.eema.backend.entity.BookingPolicyVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface BookingPolicyVersionRepository extends JpaRepository<BookingPolicyVersion,BookingPolicyVersion.Key>{List<BookingPolicyVersion> findByBookingId(UUID bookingId);}
