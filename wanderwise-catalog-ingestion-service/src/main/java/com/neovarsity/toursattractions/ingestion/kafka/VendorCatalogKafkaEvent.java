package com.neovarsity.toursattractions.ingestion.kafka;

/**
 * Simulates a Kafka catalog message (prod: consume from topic vendor.catalog.events).
 */
public record VendorCatalogKafkaEvent(String vendorCode, String rawPayload) {
}
