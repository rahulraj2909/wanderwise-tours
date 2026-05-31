package com.neovarsity.toursattractions.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class CreateReviewRequest {

    @NotNull
    private Long attractionId;

    @Min(1)
    @Max(5)
    private Integer rating;

    @Size(max = 2000)
    private String comment;
}
