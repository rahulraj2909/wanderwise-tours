package com.neovarsity.toursattractions.repository;

import com.neovarsity.toursattractions.entity.VendorActivity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface VendorActivityRepository extends JpaRepository<VendorActivity, Long> {

    List<VendorActivity> findByAttractionIdAndActiveTrue(Long attractionId);

    Optional<VendorActivity> findByAttractionIdAndVendorActivityCode(Long attractionId, String vendorActivityCode);
}
