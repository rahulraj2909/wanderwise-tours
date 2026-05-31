package com.neovarsity.toursattractions.util;

import com.neovarsity.toursattractions.dto.response.AttractionResponse;
import com.neovarsity.toursattractions.dto.response.TimeSlotResponse;
import com.neovarsity.toursattractions.entity.Attraction;
import com.neovarsity.toursattractions.entity.TimeSlot;

public final class EntityMapper {

    private EntityMapper() {
    }

    public static AttractionResponse toAttractionResponse(Attraction a) {
        return AttractionResponse.builder()
                .id(a.getId())
                .title(a.getTitle())
                .description(a.getDescription())
                .type(a.getType())
                .price(a.getPrice())
                .currency(a.getCurrency())
                .durationHours(a.getDurationHours())
                .maxGroupSize(a.getMaxGroupSize())
                .averageRating(a.getAverageRating())
                .reviewCount(a.getReviewCount())
                .imageUrl(a.getImageUrl())
                .cityId(a.getCity().getId())
                .cityName(a.getCity().getName())
                .categoryId(a.getCategory().getId())
                .categoryName(a.getCategory().getName())
                .active(a.getActive())
                .build();
    }

    public static TimeSlotResponse toTimeSlotResponse(TimeSlot slot) {
        return TimeSlotResponse.builder()
                .id(slot.getId())
                .attractionId(slot.getAttraction().getId())
                .slotDate(slot.getSlotDate())
                .startTime(slot.getStartTime())
                .availableSeats(slot.getAvailableSeats())
                .totalSeats(slot.getTotalSeats())
                .build();
    }
}
