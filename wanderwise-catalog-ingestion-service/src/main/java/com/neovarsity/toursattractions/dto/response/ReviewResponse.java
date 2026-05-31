package com.neovarsity.toursattractions.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class ReviewResponse {

    private Long id;
    private Long attractionId;
    private String userName;
    private Integer rating;
    private String comment;
    private Instant createdAt;
}
