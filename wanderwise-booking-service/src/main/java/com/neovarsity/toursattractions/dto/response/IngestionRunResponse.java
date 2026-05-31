package com.neovarsity.toursattractions.dto.response;

import com.neovarsity.toursattractions.entity.enums.IngestionRunStatus;
import lombok.Builder;

import java.time.Instant;

@Builder
public record IngestionRunResponse(
        Long id,
        String vendorCode,
        IngestionRunStatus status,
        Integer productsReceived,
        Integer productsUpserted,
        Integer activitiesUpserted,
        Integer paxTypesUpserted,
        String errorMessage,
        Instant startedAt,
        Instant finishedAt
) {
}
