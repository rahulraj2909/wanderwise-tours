package com.neovarsity.toursattractions.ingestion.model;

import com.neovarsity.toursattractions.entity.enums.AttractionType;
import lombok.Builder;

import java.math.BigDecimal;
import java.util.List;

@Builder
public record CanonicalProduct(
        String externalId,
        String title,
        String description,
        String cityCode,
        String categoryName,
        AttractionType type,
        BigDecimal basePrice,
        String currency,
        Integer durationHours,
        String imageUrl,
        BigDecimal rating,
        Integer reviewCount,
        List<CanonicalActivity> activities,
        List<CanonicalPaxType> paxTypes
) {
}
