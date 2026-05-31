package com.neovarsity.toursattractions.dto.response;

import com.neovarsity.toursattractions.entity.enums.PaymentStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
public class PaymentResponse {

    private Long id;
    private Long bookingId;
    private String bookingReference;
    private BigDecimal amount;
    private String currency;
    private PaymentStatus status;
    private String stripeSessionId;
    private Instant paidAt;
}
