package com.neovarsity.toursattractions.service;

import com.neovarsity.toursattractions.dto.response.ReconciliationResultResponse;
import com.neovarsity.toursattractions.entity.enums.PaymentStatus;
import com.neovarsity.toursattractions.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Reconciles pending payments with booking lifecycle (scheduled + admin-triggered).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentReconciliationService {

    private final PaymentRepository paymentRepository;
    private final BookingService bookingService;

    @Transactional
    public ReconciliationResultResponse reconcileExpiredBookings() {
        int pendingCount = paymentRepository.findByStatus(PaymentStatus.PENDING).size();
        log.info("Starting payment reconciliation, pending payments: {}", pendingCount);

        int expiredBookings = bookingService.expireStaleBookings();

        ReconciliationResultResponse result = ReconciliationResultResponse.builder()
                .executedAt(Instant.now())
                .pendingPaymentsReviewed(pendingCount)
                .expiredBookings(expiredBookings)
                .message(expiredBookings > 0
                        ? "Reconciliation completed; expired unpaid bookings released"
                        : "Reconciliation completed; no stale bookings found")
                .build();

        log.info("Reconciliation finished: {} bookings expired", expiredBookings);
        return result;
    }
}
