package com.neovarsity.toursattractions.service;

import com.neovarsity.toursattractions.entity.Booking;
import com.neovarsity.toursattractions.entity.Payment;
import com.neovarsity.toursattractions.entity.enums.PaymentStatus;
import com.neovarsity.toursattractions.exception.BusinessException;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import com.stripe.param.checkout.SessionCreateParams;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
@Slf4j
public class StripePaymentService {

    @Value("${stripe.enabled:false}")
    private boolean stripeEnabled;

    @Value("${stripe.webhook-secret:whsec_placeholder}")
    private String webhookSecret;

    @Value("${app.base-url:http://localhost:8081}")
    private String baseUrl;

    public String createCheckoutSession(Booking booking, Payment payment) throws StripeException {
        if (!stripeEnabled) {
            return baseUrl + "/api/v1/payments/mock-checkout?bookingRef=" + booking.getBookingReference();
        }

        long amountInPaise = booking.getTotalAmount()
                .multiply(BigDecimal.valueOf(100))
                .setScale(0, RoundingMode.HALF_UP)
                .longValue();

        SessionCreateParams params = SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.PAYMENT)
                .setSuccessUrl(baseUrl + "/api/v1/payments/success?session_id={CHECKOUT_SESSION_ID}")
                .setCancelUrl(baseUrl + "/api/v1/payments/cancel?bookingRef=" + booking.getBookingReference())
                .putMetadata("booking_reference", booking.getBookingReference())
                .addLineItem(SessionCreateParams.LineItem.builder()
                        .setQuantity(1L)
                        .setPriceData(SessionCreateParams.LineItem.PriceData.builder()
                                .setCurrency(payment.getCurrency().toLowerCase())
                                .setUnitAmount(amountInPaise)
                                .setProductData(SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                        .setName(booking.getAttractionTitle())
                                        .setDescription("Tours & Attractions booking " + booking.getBookingReference())
                                        .build())
                                .build())
                        .build())
                .build();

        Session session = Session.create(params);
        return session.getUrl();
    }

    public Event constructWebhookEvent(String payload, String sigHeader) {
        try {
            return Webhook.constructEvent(payload, sigHeader, webhookSecret);
        } catch (Exception e) {
            throw new BusinessException("Invalid Stripe webhook signature");
        }
    }

    public void logStripeDisabled() {
        log.info("Stripe integration disabled — using mock payment flow for local/demo");
    }
}
