package com.neovarsity.toursattractions.dto.response;

import lombok.Builder;

import java.math.BigDecimal;

@Builder
public record PaxTypeResponse(
        Long id,
        String code,
        String label,
        Integer minAge,
        Integer maxAge,
        BigDecimal price,
        String currency
) {
}
