package com.neovarsity.wanderwise.common.dto;

import lombok.Builder;

import java.math.BigDecimal;

@Builder
public record CatalogAttractionDto(
        Long id,
        String title,
        BigDecimal price,
        String currency,
        Integer maxGroupSize,
        Boolean active
) {
}
