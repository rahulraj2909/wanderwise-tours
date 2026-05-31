package com.neovarsity.toursattractions.config;

import com.neovarsity.toursattractions.entity.Vendor;
import com.neovarsity.toursattractions.entity.enums.IngestionChannel;
import com.neovarsity.toursattractions.repository.VendorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(5)
@RequiredArgsConstructor
public class VendorIngestionSeeder implements CommandLineRunner {

    private final VendorRepository vendorRepository;

    @Override
    public void run(String... args) {
        ensureVendor("VIATOR", "Viator Supply", IngestionChannel.DIRECT_PULL, "mock-vendors/viator-feed.json");
        ensureVendor("KLOOK", "Klook Partner", IngestionChannel.DIRECT_PULL, "mock-vendors/klook-feed.json");
        ensureVendor("GETYOURGUIDE", "GetYourGuide", IngestionChannel.KAFKA, "mock-vendors/getyourguide-kafka-event.json");
    }

    private void ensureVendor(String code, String name, IngestionChannel channel, String feed) {
        vendorRepository.findByCodeIgnoreCase(code).ifPresentOrElse(v -> {
            v.setName(name);
            v.setIngestionChannel(channel);
            v.setFeedLocation(feed);
            v.setActive(true);
            vendorRepository.save(v);
        }, () -> vendorRepository.save(Vendor.builder()
                .code(code)
                .name(name)
                .ingestionChannel(channel)
                .feedLocation(feed)
                .active(true)
                .build()));
    }
}
