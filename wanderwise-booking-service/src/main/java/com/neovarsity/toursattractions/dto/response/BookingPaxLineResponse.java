package com.neovarsity.toursattractions.dto.response;

import lombok.Builder;

import java.math.BigDecimal;

@Builder
public record BookingPaxLineResponse(
        String paxTypeCode,
        String paxTypeLabel,
        Integer quantity,
        BigDecimal unitPrice,
        BigDecimal lineTotal
) {
}
