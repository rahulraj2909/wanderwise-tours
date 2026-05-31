package com.neovarsity.toursattractions.dto.response;

import lombok.Builder;

@Builder
public record VendorActivityResponse(
        Long id,
        String vendorActivityCode,
        String title,
        String description
) {
}
