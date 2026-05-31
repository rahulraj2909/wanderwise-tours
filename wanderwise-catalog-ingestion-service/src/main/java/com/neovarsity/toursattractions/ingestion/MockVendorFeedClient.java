package com.neovarsity.toursattractions.ingestion;

import com.neovarsity.toursattractions.entity.Vendor;
import com.neovarsity.toursattractions.exception.BusinessException;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Component
public class MockVendorFeedClient {

    public String fetchFeed(Vendor vendor) {
        String location = vendor.getFeedLocation();
        if (location == null || location.isBlank()) {
            throw new BusinessException("Vendor feed location not configured: " + vendor.getCode());
        }
        try {
            ClassPathResource resource = new ClassPathResource(location);
            return resource.getContentAsString(StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new BusinessException("Failed to load mock feed for " + vendor.getCode() + ": " + e.getMessage());
        }
    }
}
