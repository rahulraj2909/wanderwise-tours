package com.neovarsity.toursattractions.repository;

import com.neovarsity.toursattractions.entity.Payment;
import com.neovarsity.toursattractions.entity.enums.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByStripeSessionId(String stripeSessionId);

    Optional<Payment> findByBookingId(Long bookingId);

    List<Payment> findByStatus(PaymentStatus status);
}
