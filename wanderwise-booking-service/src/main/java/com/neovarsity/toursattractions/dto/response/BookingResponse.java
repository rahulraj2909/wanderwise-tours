package com.neovarsity.toursattractions.dto.response;

import com.neovarsity.toursattractions.entity.enums.BookingStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
public class BookingResponse {

    private Long id;
    private String bookingReference;
    private Long customerId;
    private Long attractionId;
    private String attractionTitle;
    private Long timeSlotId;
    private LocalDate visitDate;
    private Integer guests;
    private BigDecimal totalAmount;
    private BookingStatus status;
    private String checkoutUrl;
    private Instant createdAt;
    private List<BookingPaxLineResponse> paxLines;
}
