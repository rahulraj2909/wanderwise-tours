package com.neovarsity.toursattractions.controller.admin;

import com.neovarsity.toursattractions.dto.response.ApiResponse;
import com.neovarsity.toursattractions.dto.response.ReconciliationResultResponse;
import com.neovarsity.toursattractions.service.AuthService;
import com.neovarsity.toursattractions.service.PaymentReconciliationService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Admin API for payment/booking reconciliation (same logic as scheduled cron).
 */
@RestController
@RequestMapping("/api/v1/admin/bookings/reconciliation")
@RequiredArgsConstructor
public class BookingReconciliationController {

    private final AuthService authService;
    private final PaymentReconciliationService paymentReconciliationService;

    /**
     * Manually triggers the same job as {@link com.neovarsity.toursattractions.scheduler.BookingReconciliationScheduler}.
     * Requires an authenticated administrator.
     */
    @PostMapping("/expired")
    public ApiResponse<ReconciliationResultResponse> reconcileExpiredBookings(HttpSession session) {
        authService.requireAdmin(session);
        ReconciliationResultResponse result = paymentReconciliationService.reconcileExpiredBookings();
        return ApiResponse.ok(result.getMessage(), result);
    }
}
