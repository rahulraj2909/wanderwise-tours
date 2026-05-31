package com.neovarsity.toursattractions.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class ReconciliationResultResponse {

    private Instant executedAt;
    private int pendingPaymentsReviewed;
    private int expiredBookings;
    private String message;
}
