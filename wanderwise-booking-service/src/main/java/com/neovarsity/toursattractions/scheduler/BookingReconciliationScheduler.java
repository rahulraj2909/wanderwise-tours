package com.neovarsity.toursattractions.scheduler;

import com.neovarsity.toursattractions.service.PaymentReconciliationService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class BookingReconciliationScheduler {

    private final PaymentReconciliationService reconciliationService;

    /** Runs every 15 minutes — expires unpaid bookings and syncs payment state. */
    @Scheduled(cron = "${scheduler.reconciliation-cron:0 */15 * * * *}")
    public void reconcileExpiredBookings() {
        reconciliationService.reconcileExpiredBookings();
    }
}
