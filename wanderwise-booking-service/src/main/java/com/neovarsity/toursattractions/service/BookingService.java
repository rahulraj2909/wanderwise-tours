package com.neovarsity.toursattractions.service;

import com.neovarsity.toursattractions.client.CatalogClient;
import com.neovarsity.toursattractions.dto.request.CreateBookingRequest;
import com.neovarsity.toursattractions.dto.request.PaxSelectionRequest;
import com.neovarsity.toursattractions.dto.response.BookingResponse;
import com.neovarsity.toursattractions.entity.*;
import com.neovarsity.toursattractions.entity.enums.BookingStatus;
import com.neovarsity.toursattractions.entity.enums.PaymentStatus;
import com.neovarsity.toursattractions.exception.BusinessException;
import com.neovarsity.toursattractions.exception.ResourceNotFoundException;
import com.neovarsity.toursattractions.repository.BookingRepository;
import com.neovarsity.toursattractions.repository.PaymentRepository;
import com.neovarsity.toursattractions.util.EntityMapper;
import com.neovarsity.wanderwise.common.WanderwiseConstants;
import com.neovarsity.wanderwise.common.dto.CatalogAttractionDto;
import com.neovarsity.wanderwise.common.dto.CatalogPaxTypeDto;
import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class BookingService {

    private final BookingRepository bookingRepository;
    private final PaymentRepository paymentRepository;
    private final UserService userService;
    private final CatalogClient catalogClient;
    private final StripePaymentService stripePaymentService;

    @Value("${booking.payment-timeout-minutes:30}")
    private int paymentTimeoutMinutes;

    @Transactional
    public BookingResponse createBooking(CreateBookingRequest request, Long customerId) throws StripeException {
        User customer = userService.getById(customerId);
        CatalogAttractionDto attraction = catalogClient.getAttraction(request.getAttractionId());
        if (Boolean.FALSE.equals(attraction.active())) {
            throw new BusinessException("This tour is not available for booking");
        }

        int guestCount = resolveGuestCount(request);
        Long timeSlotId = request.getTimeSlotId();
        if (timeSlotId != null) {
            catalogClient.reserveSeats(timeSlotId, guestCount);
        }

        PaxPricing pricing = resolvePricing(attraction, request);
        String reference = WanderwiseConstants.BOOKING_REFERENCE_PREFIX
                + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        Booking booking = Booking.builder()
                .bookingReference(reference)
                .customer(customer)
                .attractionId(attraction.id())
                .attractionTitle(attraction.title())
                .timeSlotId(timeSlotId)
                .visitDate(request.getVisitDate())
                .guests(guestCount)
                .unitPrice(pricing.unitPrice())
                .totalAmount(pricing.total())
                .currency(attraction.currency())
                .status(BookingStatus.PENDING_PAYMENT)
                .contactEmail(request.getContactEmail() != null ? request.getContactEmail() : customer.getEmail())
                .contactPhone(request.getContactPhone())
                .build();
        booking = bookingRepository.save(booking);

        for (BookingPaxLine line : pricing.paxLines()) {
            line.setBooking(booking);
            booking.getPaxLines().add(line);
        }
        if (!booking.getPaxLines().isEmpty()) {
            booking = bookingRepository.save(booking);
        }

        Payment payment = Payment.builder()
                .booking(booking)
                .amount(pricing.total())
                .currency(attraction.currency())
                .status(PaymentStatus.PENDING)
                .build();
        payment = paymentRepository.save(payment);
        booking.setPayment(payment);

        String checkoutUrl = stripePaymentService.createCheckoutSession(booking, payment);
        return EntityMapper.toBookingResponse(booking, checkoutUrl);
    }

    private int resolveGuestCount(CreateBookingRequest request) {
        if (request.getPaxSelections() != null && !request.getPaxSelections().isEmpty()) {
            return request.getPaxSelections().stream()
                    .mapToInt(PaxSelectionRequest::getQuantity)
                    .sum();
        }
        return request.getGuests() != null ? request.getGuests() : 1;
    }

    private PaxPricing resolvePricing(CatalogAttractionDto attraction, CreateBookingRequest request) {
        if (request.getPaxSelections() == null || request.getPaxSelections().isEmpty()) {
            int guests = request.getGuests() != null ? request.getGuests() : 1;
            BigDecimal total = attraction.price().multiply(BigDecimal.valueOf(guests));
            return new PaxPricing(attraction.price(), total, List.of());
        }
        List<BookingPaxLine> lines = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;
        for (PaxSelectionRequest sel : request.getPaxSelections()) {
            CatalogPaxTypeDto pax = catalogClient.findPaxByCode(attraction.id(), sel.getPaxTypeCode());
            BigDecimal lineTotal = pax.price().multiply(BigDecimal.valueOf(sel.getQuantity()));
            lines.add(BookingPaxLine.builder()
                    .paxTypeCode(pax.code())
                    .paxTypeLabel(pax.label())
                    .quantity(sel.getQuantity())
                    .unitPrice(pax.price())
                    .lineTotal(lineTotal)
                    .build());
            total = total.add(lineTotal);
        }
        int guestSum = request.getPaxSelections().stream().mapToInt(PaxSelectionRequest::getQuantity).sum();
        BigDecimal unit = guestSum == 0 ? attraction.price()
                : total.divide(BigDecimal.valueOf(guestSum), 2, java.math.RoundingMode.HALF_UP);
        return new PaxPricing(unit, total, lines);
    }

    private record PaxPricing(BigDecimal unitPrice, BigDecimal total, List<BookingPaxLine> paxLines) {
    }

    @Transactional(readOnly = true)
    public BookingResponse getByReference(String reference) {
        Booking booking = bookingRepository.findByBookingReference(reference)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found: " + reference));
        return EntityMapper.toBookingResponse(booking, null);
    }

    @Transactional(readOnly = true)
    public Page<BookingResponse> listByCustomer(Long customerId, Pageable pageable) {
        return bookingRepository.findByCustomerId(customerId, pageable)
                .map(b -> EntityMapper.toBookingResponse(b, null));
    }

    @Transactional
    public BookingResponse confirmPayment(String bookingReference, String stripeSessionId) {
        Booking booking = bookingRepository.findByBookingReference(bookingReference)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));

        if (booking.getStatus() == BookingStatus.CONFIRMED) {
            return EntityMapper.toBookingResponse(booking, null);
        }

        Payment payment = paymentRepository.findByBookingId(booking.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found"));

        payment.setStatus(PaymentStatus.SUCCEEDED);
        payment.setStripeSessionId(stripeSessionId);
        payment.setPaidAt(Instant.now());
        booking.setStatus(BookingStatus.CONFIRMED);

        return EntityMapper.toBookingResponse(booking, null);
    }

    @Transactional
    public int expireStaleBookings() {
        Instant cutoff = Instant.now().minusSeconds(paymentTimeoutMinutes * 60L);
        var stale = bookingRepository.findByStatusAndCreatedAtBefore(BookingStatus.PENDING_PAYMENT, cutoff);
        stale.forEach(booking -> {
            booking.setStatus(BookingStatus.EXPIRED);
            catalogClient.releaseSeats(booking.getTimeSlotId(), booking.getGuests());
            paymentRepository.findByBookingId(booking.getId()).ifPresent(p -> {
                if (p.getStatus() == PaymentStatus.PENDING) {
                    p.setStatus(PaymentStatus.FAILED);
                }
            });
            log.info("Expired unpaid booking {}", booking.getBookingReference());
        });
        return stale.size();
    }

    @Transactional
    public void handleCheckoutCompleted(Session session) {
        String ref = session.getMetadata().get("booking_reference");
        confirmPayment(ref, session.getId());
    }
}
