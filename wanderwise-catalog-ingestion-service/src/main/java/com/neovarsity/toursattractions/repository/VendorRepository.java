package com.neovarsity.toursattractions.repository;

import com.neovarsity.toursattractions.entity.Vendor;
import com.neovarsity.toursattractions.entity.enums.IngestionChannel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface VendorRepository extends JpaRepository<Vendor, Long> {

    Optional<Vendor> findByCodeIgnoreCase(String code);

    List<Vendor> findByActiveTrueAndIngestionChannel(IngestionChannel channel);
}
