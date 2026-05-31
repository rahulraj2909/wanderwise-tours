package com.neovarsity.toursattractions.dto.response;

import com.neovarsity.toursattractions.entity.enums.IngestionChannel;
import lombok.Builder;

@Builder
public record VendorResponse(
        Long id,
        String code,
        String name,
        IngestionChannel ingestionChannel,
        String feedLocation,
        Boolean active
) {
}
