package com.neovarsity.toursattractions.controller;

import com.neovarsity.toursattractions.dto.request.CreateBookingRequest;
import com.neovarsity.toursattractions.dto.response.ApiResponse;
import com.neovarsity.toursattractions.dto.response.BookingResponse;
import com.neovarsity.toursattractions.entity.User;
import com.neovarsity.toursattractions.service.AuthService;
import com.neovarsity.toursattractions.service.BookingService;
import com.stripe.exception.StripeException;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;
    private final AuthService authService;

    /**
     * Flagship API: create booking with seat reservation and payment checkout URL.
     */
    @PostMapping
    public ApiResponse<BookingResponse> create(@Valid @RequestBody CreateBookingRequest request,
                                               HttpSession session) throws StripeException {
        User user = authService.requireUser(session);
        return ApiResponse.ok("Booking initiated — complete payment via checkoutUrl",
                bookingService.createBooking(request, user.getId()));
    }

    @GetMapping("/{reference}")
    public ApiResponse<BookingResponse> get(@PathVariable String reference) {
        return ApiResponse.ok(bookingService.getByReference(reference));
    }

    @GetMapping("/customer/{customerId}")
    public ApiResponse<Page<BookingResponse>> listByCustomer(
            @PathVariable Long customerId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            HttpSession session) {
        User user = authService.requireUser(session);
        if (!user.getId().equals(customerId)) {
            throw new com.neovarsity.toursattractions.exception.BusinessException("Access denied");
        }
        return ApiResponse.ok(bookingService.listByCustomer(customerId, PageRequest.of(page, size)));
    }
}
