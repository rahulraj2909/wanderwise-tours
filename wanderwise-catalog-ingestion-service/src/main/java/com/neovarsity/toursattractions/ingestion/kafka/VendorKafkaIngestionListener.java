package com.neovarsity.toursattractions.ingestion.kafka;

import com.neovarsity.toursattractions.service.VendorIngestionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class VendorKafkaIngestionListener {

    private final VendorIngestionService vendorIngestionService;

    @Async
    @EventListener
    public void onCatalogEvent(VendorCatalogKafkaEvent event) {
        log.info("Kafka-sim ingestion event for vendor {}", event.vendorCode());
        vendorIngestionService.ingestKafkaPayload(event.vendorCode(), event.rawPayload());
    }
}
