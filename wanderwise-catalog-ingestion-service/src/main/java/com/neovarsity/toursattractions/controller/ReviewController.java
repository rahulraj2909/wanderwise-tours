package com.neovarsity.toursattractions.controller;

import com.neovarsity.toursattractions.client.BookingSessionClient;
import com.neovarsity.toursattractions.dto.request.CreateReviewRequest;
import com.neovarsity.toursattractions.dto.response.ApiResponse;
import com.neovarsity.toursattractions.dto.response.ReviewResponse;
import com.neovarsity.toursattractions.entity.Review;
import com.neovarsity.toursattractions.exception.BusinessException;
import com.neovarsity.toursattractions.service.ReviewService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;
    private final BookingSessionClient bookingSessionClient;

    @PostMapping
    public ApiResponse<Review> create(@Valid @RequestBody CreateReviewRequest request, HttpServletRequest httpRequest) {
        BookingSessionClient.BookingUser user = bookingSessionClient.resolveUser(httpRequest)
                .orElseThrow(() -> new BusinessException("Please log in to post a review"));
        return ApiResponse.ok("Review submitted", reviewService.create(request, user.id(), user.name()));
    }

    @GetMapping("/attraction/{attractionId}")
    public ApiResponse<Page<ReviewResponse>> list(
            @PathVariable Long attractionId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.ok(reviewService.listByAttractionDto(attractionId, PageRequest.of(page, size)));
    }
}
