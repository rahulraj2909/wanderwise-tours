package com.neovarsity.toursattractions.service;

import com.neovarsity.toursattractions.dto.request.CreateReviewRequest;
import com.neovarsity.toursattractions.dto.response.ReviewResponse;
import com.neovarsity.toursattractions.entity.Attraction;
import com.neovarsity.toursattractions.entity.Review;
import com.neovarsity.toursattractions.exception.ResourceNotFoundException;
import com.neovarsity.toursattractions.repository.AttractionRepository;
import com.neovarsity.toursattractions.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final AttractionRepository attractionRepository;

    @Transactional
    @CacheEvict(value = {"attractions", "attractionDetail"}, allEntries = true)
    public Review create(CreateReviewRequest request, Long userId, String userName) {
        Attraction attraction = attractionRepository.findById(request.getAttractionId())
                .orElseThrow(() -> new ResourceNotFoundException("Attraction not found"));

        Review review = Review.builder()
                .userId(userId)
                .userName(userName != null ? userName : "Guest")
                .attraction(attraction)
                .rating(request.getRating())
                .comment(request.getComment())
                .build();
        review = reviewRepository.save(review);
        recalculateRating(attraction);
        return review;
    }

    @Transactional(readOnly = true)
    public Page<ReviewResponse> listByAttractionDto(Long attractionId, Pageable pageable) {
        return reviewRepository.findByAttractionId(attractionId, pageable)
                .map(r -> ReviewResponse.builder()
                        .id(r.getId())
                        .attractionId(r.getAttraction().getId())
                        .userName(r.getUserName())
                        .rating(r.getRating())
                        .comment(r.getComment())
                        .createdAt(r.getCreatedAt())
                        .build());
    }

    private void recalculateRating(Attraction attraction) {
        Page<Review> all = reviewRepository.findByAttractionId(attraction.getId(), Pageable.unpaged());
        if (all.isEmpty()) {
            return;
        }
        double avg = all.stream().mapToInt(Review::getRating).average().orElse(0);
        attraction.setAverageRating(BigDecimal.valueOf(avg).setScale(2, RoundingMode.HALF_UP));
        attraction.setReviewCount((int) all.getTotalElements());
        attractionRepository.save(attraction);
    }
}
