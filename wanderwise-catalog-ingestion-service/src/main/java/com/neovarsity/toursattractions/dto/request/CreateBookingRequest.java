package com.neovarsity.toursattractions.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class CreateBookingRequest {

    @NotNull
    private Long attractionId;

    private Long timeSlotId;

    @NotNull
    private LocalDate visitDate;

    @Min(1)
    @Max(50)
    private Integer guests;

    @Email
    private String contactEmail;

    private String contactPhone;

    /** When set, total is computed per pax type (vendor-ingested products). */
    private List<PaxSelectionRequest> paxSelections;
}
