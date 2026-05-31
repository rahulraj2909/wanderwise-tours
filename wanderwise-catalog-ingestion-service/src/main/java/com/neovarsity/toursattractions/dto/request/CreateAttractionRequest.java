package com.neovarsity.toursattractions.dto.request;

import com.neovarsity.toursattractions.entity.enums.AttractionType;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CreateAttractionRequest {

    @NotBlank
    private String title;

    private String description;

    @NotNull
    private AttractionType type;

    @NotNull
    @DecimalMin("0.01")
    private BigDecimal price;

    private String currency;

    @Min(1)
    private Integer durationHours;

    private Integer maxGroupSize;

    private String imageUrl;

    @NotNull
    private Long cityId;

    @NotNull
    private Long categoryId;

    private Long operatorId;
}
