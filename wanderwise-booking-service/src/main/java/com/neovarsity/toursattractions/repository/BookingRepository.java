package com.neovarsity.toursattractions.repository;

import com.neovarsity.toursattractions.entity.Booking;
import com.neovarsity.toursattractions.entity.enums.BookingStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    Optional<Booking> findByBookingReference(String bookingReference);

    Page<Booking> findByCustomerId(Long customerId, Pageable pageable);

    List<Booking> findByStatusAndCreatedAtBefore(BookingStatus status, Instant createdBefore);
}
