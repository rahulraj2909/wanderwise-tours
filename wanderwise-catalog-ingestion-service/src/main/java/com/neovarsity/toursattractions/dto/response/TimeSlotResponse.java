package com.neovarsity.toursattractions.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
@Builder
public class TimeSlotResponse {

    private Long id;
    private Long attractionId;
    private LocalDate slotDate;
    private LocalTime startTime;
    private Integer availableSeats;
    private Integer totalSeats;
}
