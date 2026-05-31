package com.neovarsity.toursattractions.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class PaxSelectionRequest {

    @NotBlank
    private String paxTypeCode;

    @Min(1)
    private Integer quantity;
}
