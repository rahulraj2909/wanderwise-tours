package com.neovarsity.toursattractions.ingestion.model;

import lombok.Builder;

@Builder
public record CanonicalActivity(
        String externalCode,
        String title,
        String description
) {
}
