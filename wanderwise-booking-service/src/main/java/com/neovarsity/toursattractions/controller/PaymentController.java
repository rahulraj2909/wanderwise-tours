package com.neovarsity.toursattractions.controller;

import com.neovarsity.toursattractions.dto.response.ApiResponse;
import com.neovarsity.toursattractions.dto.response.BookingResponse;
import com.neovarsity.toursattractions.service.BookingService;
import com.neovarsity.toursattractions.service.StripePaymentService;
import com.stripe.model.Event;
import com.stripe.model.checkout.Session;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
@Slf4j
public class PaymentController {

    private final BookingService bookingService;
    private final StripePaymentService stripePaymentService;

    @Value("${stripe.enabled:false}")
    private boolean stripeEnabled;

    /** Mock checkout for local/demo when Stripe is disabled. */
    @GetMapping("/mock-checkout")
    public ApiResponse<BookingResponse> mockCheckout(@RequestParam String bookingRef) {
        stripePaymentService.logStripeDisabled();
        return ApiResponse.ok("Payment simulated",
                bookingService.confirmPayment(bookingRef, "mock_session_" + bookingRef));
    }

    @GetMapping("/success")
    public ApiResponse<BookingResponse> success(@RequestParam(required = false) String session_id,
                                                @RequestParam(required = false) String bookingRef) {
        if (bookingRef != null) {
            return ApiResponse.ok(bookingService.confirmPayment(bookingRef, session_id));
        }
        return ApiResponse.ok("Payment success — configure bookingRef for mock mode", null);
    }

    @PostMapping("/webhook/stripe")
    public ResponseEntity<String> stripeWebhook(
            @RequestBody String payload,
            @RequestHeader(value = "Stripe-Signature", required = false) String sigHeader) {

        if (!stripeEnabled) {
            return ResponseEntity.ok("stripe disabled");
        }

        Event event = stripePaymentService.constructWebhookEvent(payload, sigHeader);
        if ("checkout.session.completed".equals(event.getType())) {
            Session session = (Session) event.getDataObjectDeserializer()
                    .getObject().orElse(null);
            if (session != null) {
                bookingService.handleCheckoutCompleted(session);
            }
        }
        return ResponseEntity.ok("received");
    }
}
