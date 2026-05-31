package com.neovarsity.toursattractions.dto.response;

import com.neovarsity.toursattractions.entity.enums.AttractionType;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class AttractionResponse {

    private Long id;
    private String title;
    private String description;
    private AttractionType type;
    private BigDecimal price;
    private String currency;
    private Integer durationHours;
    private Integer maxGroupSize;
    private BigDecimal averageRating;
    private Integer reviewCount;
    private String imageUrl;
    private Long cityId;
    private String cityName;
    private Long categoryId;
    private String categoryName;
}
