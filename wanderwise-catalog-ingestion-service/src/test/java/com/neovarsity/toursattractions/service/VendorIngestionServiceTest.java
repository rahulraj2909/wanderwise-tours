package com.neovarsity.toursattractions.service;

import com.neovarsity.toursattractions.repository.AttractionRepository;
import com.neovarsity.toursattractions.repository.PaxTypeRepository;
import com.neovarsity.toursattractions.repository.VendorActivityRepository;
import com.neovarsity.toursattractions.repository.VendorRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("h2")
class VendorIngestionServiceTest {

    @Autowired
    private VendorIngestionService vendorIngestionService;
    @Autowired
    private VendorRepository vendorRepository;
    @Autowired
    private AttractionRepository attractionRepository;
    @Autowired
    private VendorActivityRepository vendorActivityRepository;
    @Autowired
    private PaxTypeRepository paxTypeRepository;

    @Test
    void directPullUpsertsViatorCatalog() {
        long attractionsBefore = attractionRepository.count();
        var run = vendorIngestionService.ingestDirectPull("VIATOR");
        assertThat(run.getStatus().name()).isEqualTo("SUCCESS");
        assertThat(attractionRepository.count()).isGreaterThan(attractionsBefore);
        assertThat(vendorActivityRepository.count()).isGreaterThan(0);
        assertThat(paxTypeRepository.count()).isGreaterThan(0);
        assertThat(vendorRepository.findByCodeIgnoreCase("VIATOR")).isPresent();
    }
}
