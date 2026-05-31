package com.neovarsity.wanderwise.common.dto;

import lombok.Builder;

@Builder
public record SlotActionResultDto(
        Long timeSlotId,
        int availableSeats,
        boolean success,
        String message
) {
}
