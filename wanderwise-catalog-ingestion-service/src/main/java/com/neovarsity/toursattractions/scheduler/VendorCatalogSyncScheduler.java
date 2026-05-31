package com.neovarsity.toursattractions.scheduler;

import com.neovarsity.toursattractions.service.VendorIngestionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduled catalog sync for DIRECT_PULL vendors.
 * Ingest cron — new products only; update cron — refresh existing products (prices, copy, pax).
 */
@Component
@ConditionalOnProperty(name = "ingestion.scheduled.enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
@Slf4j
public class VendorCatalogSyncScheduler {

    private final VendorIngestionService vendorIngestionService;

    @Value("${ingestion.scheduled-ingest-enabled:true}")
    private boolean ingestEnabled;

    @Value("${ingestion.scheduled-update-enabled:true}")
    private boolean updateEnabled;

    /** New vendor products only — default 02:00 daily */
    @Scheduled(cron = "${ingestion.scheduled-ingest-cron:0 0 2 * * *}")
    public void scheduledIngest() {
        if (!ingestEnabled) {
            return;
        }
        log.info("Starting scheduled catalog ingest (new products only)");
        var runs = vendorIngestionService.runScheduledIngestForAllDirectPullVendors();
        log.info("Scheduled ingest finished: {} vendor run(s)", runs.size());
    }

    /** Update existing catalog rows — default every 6 hours */
    @Scheduled(cron = "${ingestion.scheduled-update-cron:0 0 */6 * * *}")
    public void scheduledUpdate() {
        if (!updateEnabled) {
            return;
        }
        log.info("Starting scheduled catalog update (existing products only)");
        var runs = vendorIngestionService.runScheduledUpdateForAllDirectPullVendors();
        log.info("Scheduled update finished: {} vendor run(s)", runs.size());
    }
}
