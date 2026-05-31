package com.neovarsity.wanderwise.common.dto;

import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
public class ReserveSeatsRequest {

    @Min(1)
    private int guests;
}
