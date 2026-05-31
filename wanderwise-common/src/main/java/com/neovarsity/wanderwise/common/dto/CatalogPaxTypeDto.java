package com.neovarsity.wanderwise.common.dto;

import lombok.Builder;

import java.math.BigDecimal;

@Builder
public record CatalogPaxTypeDto(
        Long id,
        String code,
        String label,
        BigDecimal price,
        String currency
) {
}
