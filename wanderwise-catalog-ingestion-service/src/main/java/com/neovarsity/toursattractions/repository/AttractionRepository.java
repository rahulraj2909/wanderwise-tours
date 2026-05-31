package com.neovarsity.toursattractions.repository;

import com.neovarsity.toursattractions.entity.Attraction;
import com.neovarsity.toursattractions.entity.enums.AttractionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface AttractionRepository extends JpaRepository<Attraction, Long> {

    boolean existsByTitleAndCity_Id(String title, Long cityId);

    Optional<Attraction> findByTitleAndCity_Id(String title, Long cityId);

    Optional<Attraction> findByVendor_IdAndVendorExternalId(Long vendorId, String vendorExternalId);

    Page<Attraction> findByActiveTrue(Pageable pageable);

    @Query("""
            SELECT a FROM Attraction a
            WHERE a.active = true
              AND (:cityId IS NULL OR a.city.id = :cityId)
              AND (:categoryId IS NULL OR a.category.id = :categoryId)
              AND (:type IS NULL OR a.type = :type)
              AND (:keyword IS NULL OR LOWER(a.title) LIKE LOWER(CONCAT('%', :keyword, '%'))
                   OR LOWER(a.description) LIKE LOWER(CONCAT('%', :keyword, '%')))
            """)
    Page<Attraction> searchActive(
            @Param("cityId") Long cityId,
            @Param("categoryId") Long categoryId,
            @Param("type") AttractionType type,
            @Param("keyword") String keyword,
            Pageable pageable);

    @Query("""
            SELECT a FROM Attraction a
            WHERE (:keyword IS NULL OR :keyword = '' OR LOWER(a.title) LIKE LOWER(CONCAT('%', :keyword, '%'))
                   OR LOWER(a.description) LIKE LOWER(CONCAT('%', :keyword, '%')))
            """)
    Page<Attraction> searchAdminCatalog(@Param("keyword") String keyword, Pageable pageable);
}
