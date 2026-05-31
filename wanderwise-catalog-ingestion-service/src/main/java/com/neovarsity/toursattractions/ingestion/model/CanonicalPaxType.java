package com.neovarsity.toursattractions.ingestion.model;

import lombok.Builder;

import java.math.BigDecimal;

@Builder
public record CanonicalPaxType(
        String code,
        String label,
        Integer minAge,
        Integer maxAge,
        BigDecimal price,
        String currency,
        String linkedActivityCode
) {
}
