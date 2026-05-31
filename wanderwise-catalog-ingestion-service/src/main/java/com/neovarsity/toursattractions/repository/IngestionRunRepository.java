package com.neovarsity.toursattractions.repository;

import com.neovarsity.toursattractions.entity.IngestionRun;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface IngestionRunRepository extends JpaRepository<IngestionRun, Long> {

    List<IngestionRun> findByVendorIdOrderByStartedAtDesc(Long vendorId);
}
