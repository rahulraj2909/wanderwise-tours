package com.neovarsity.toursattractions.util;

import com.neovarsity.toursattractions.dto.response.BookingPaxLineResponse;
import com.neovarsity.toursattractions.dto.response.BookingResponse;
import com.neovarsity.toursattractions.dto.response.PaymentResponse;
import com.neovarsity.toursattractions.entity.Booking;
import com.neovarsity.toursattractions.entity.BookingPaxLine;
import com.neovarsity.toursattractions.entity.Payment;

import java.util.List;

public final class EntityMapper {

    private EntityMapper() {
    }

    public static BookingResponse toBookingResponse(Booking booking, String checkoutUrl) {
        return BookingResponse.builder()
                .id(booking.getId())
                .bookingReference(booking.getBookingReference())
                .customerId(booking.getCustomer().getId())
                .attractionId(booking.getAttractionId())
                .attractionTitle(booking.getAttractionTitle())
                .timeSlotId(booking.getTimeSlotId())
                .visitDate(booking.getVisitDate())
                .guests(booking.getGuests())
                .totalAmount(booking.getTotalAmount())
                .status(booking.getStatus())
                .checkoutUrl(checkoutUrl)
                .createdAt(booking.getCreatedAt())
                .paxLines(toPaxLineResponses(booking.getPaxLines()))
                .build();
    }

    private static List<BookingPaxLineResponse> toPaxLineResponses(List<BookingPaxLine> lines) {
        if (lines == null || lines.isEmpty()) {
            return List.of();
        }
        return lines.stream()
                .map(line -> BookingPaxLineResponse.builder()
                        .paxTypeCode(line.getPaxTypeCode())
                        .paxTypeLabel(line.getPaxTypeLabel())
                        .quantity(line.getQuantity())
                        .unitPrice(line.getUnitPrice())
                        .lineTotal(line.getLineTotal())
                        .build())
                .toList();
    }

    public static PaymentResponse toPaymentResponse(Payment payment) {
        return PaymentResponse.builder()
                .id(payment.getId())
                .bookingId(payment.getBooking().getId())
                .bookingReference(payment.getBooking().getBookingReference())
                .amount(payment.getAmount())
                .currency(payment.getCurrency())
                .status(payment.getStatus())
                .stripeSessionId(payment.getStripeSessionId())
                .paidAt(payment.getPaidAt())
                .build();
    }
}
