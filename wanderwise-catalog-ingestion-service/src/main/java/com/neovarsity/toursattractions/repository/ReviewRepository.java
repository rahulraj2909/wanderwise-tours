package com.neovarsity.toursattractions.repository;

import com.neovarsity.toursattractions.entity.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    Page<Review> findByAttractionId(Long attractionId, Pageable pageable);
}
