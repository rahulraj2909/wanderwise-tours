package com.neovarsity.toursattractions.repository;

import com.neovarsity.toursattractions.entity.PaxType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PaxTypeRepository extends JpaRepository<PaxType, Long> {

    List<PaxType> findByAttractionIdAndActiveTrueOrderByCodeAsc(Long attractionId);

    Optional<PaxType> findByAttractionIdAndCodeIgnoreCase(Long attractionId, String code);
}
