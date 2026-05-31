package com.neovarsity.toursattractions.dto.response;

import com.neovarsity.toursattractions.entity.enums.IngestionRunStatus;
import com.neovarsity.toursattractions.entity.enums.IngestionRunType;
import lombok.Builder;

import java.time.Instant;

@Builder
public record IngestionRunResponse(
        Long id,
        String vendorCode,
        IngestionRunType runType,
        IngestionRunStatus status,
        Integer productsReceived,
        Integer productsCreated,
        Integer productsUpdated,
        Integer productsUpserted,
        Integer activitiesUpserted,
        Integer paxTypesUpserted,
        String errorMessage,
        Instant startedAt,
        Instant finishedAt
) {
}
