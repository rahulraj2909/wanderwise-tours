package com.neovarsity.toursattractions.service;

import com.neovarsity.toursattractions.entity.*;
import com.neovarsity.toursattractions.entity.enums.AttractionType;
import com.neovarsity.toursattractions.entity.enums.CatalogSyncMode;
import com.neovarsity.toursattractions.entity.enums.IngestionChannel;
import com.neovarsity.toursattractions.entity.enums.IngestionRunStatus;
import com.neovarsity.toursattractions.entity.enums.IngestionRunType;
import com.neovarsity.toursattractions.exception.BusinessException;
import com.neovarsity.toursattractions.exception.ResourceNotFoundException;
import com.neovarsity.toursattractions.ingestion.MockVendorFeedClient;
import com.neovarsity.toursattractions.ingestion.VendorMapperRegistry;
import com.neovarsity.toursattractions.ingestion.VendorProductMapper;
import com.neovarsity.toursattractions.ingestion.kafka.VendorCatalogKafkaEvent;
import com.neovarsity.toursattractions.ingestion.model.CanonicalActivity;
import com.neovarsity.toursattractions.ingestion.model.CanonicalPaxType;
import com.neovarsity.toursattractions.ingestion.model.CanonicalProduct;
import com.neovarsity.toursattractions.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class VendorIngestionService {

    private final VendorRepository vendorRepository;
    private final IngestionRunRepository ingestionRunRepository;
    private final AttractionRepository attractionRepository;
    private final CityRepository cityRepository;
    private final CategoryRepository categoryRepository;
    private final VendorActivityRepository vendorActivityRepository;
    private final PaxTypeRepository paxTypeRepository;
    private final TimeSlotRepository timeSlotRepository;
    private final MockVendorFeedClient mockVendorFeedClient;
    private final VendorMapperRegistry mapperRegistry;
    private final ApplicationEventPublisher eventPublisher;

    /** Full sync: create new products and update existing ones. */
    @Transactional
    public IngestionRun ingestDirectPull(String vendorCode) {
        return syncDirectPull(vendorCode, CatalogSyncMode.FULL, IngestionRunType.MANUAL_PULL);
    }

    /** Ingest only: add new vendor products, skip rows that already exist. */
    @Transactional
    public IngestionRun ingestNewProducts(String vendorCode) {
        return syncDirectPull(vendorCode, CatalogSyncMode.INGEST_ONLY, IngestionRunType.MANUAL_INGEST);
    }

    /** Update only: refresh existing products from feed, skip new IDs. */
    @Transactional
    public IngestionRun updateExistingProducts(String vendorCode) {
        return syncDirectPull(vendorCode, CatalogSyncMode.UPDATE_ONLY, IngestionRunType.MANUAL_UPDATE);
    }

    @Transactional
    public List<IngestionRun> runScheduledIngestForAllDirectPullVendors() {
        return runForVendors(
                vendorRepository.findByActiveTrueAndIngestionChannel(IngestionChannel.DIRECT_PULL),
                CatalogSyncMode.INGEST_ONLY,
                IngestionRunType.SCHEDULED_INGEST);
    }

    @Transactional
    public List<IngestionRun> runScheduledUpdateForAllDirectPullVendors() {
        return runForVendors(
                vendorRepository.findByActiveTrueAndIngestionChannel(IngestionChannel.DIRECT_PULL),
                CatalogSyncMode.UPDATE_ONLY,
                IngestionRunType.SCHEDULED_UPDATE);
    }

    @Transactional
    public IngestionRun ingestKafkaPayload(String vendorCode, String rawPayload) {
        Vendor vendor = getVendor(vendorCode);
        if (vendor.getIngestionChannel() != IngestionChannel.KAFKA) {
            throw new BusinessException(vendorCode + " uses " + vendor.getIngestionChannel() + ", not KAFKA");
        }
        return runIngestion(vendor, rawPayload, CatalogSyncMode.FULL, IngestionRunType.KAFKA_EVENT);
    }

    public void publishSimulatedKafkaEvent(String vendorCode) {
        Vendor vendor = getVendor(vendorCode);
        if (vendor.getIngestionChannel() != IngestionChannel.KAFKA) {
            throw new BusinessException("Vendor is not configured for KAFKA: " + vendorCode);
        }
        String raw = mockVendorFeedClient.fetchFeed(vendor);
        eventPublisher.publishEvent(new VendorCatalogKafkaEvent(vendorCode.toUpperCase(), raw));
    }

    @Transactional(readOnly = true)
    public List<IngestionRun> listRuns(String vendorCode) {
        Vendor vendor = getVendor(vendorCode);
        return ingestionRunRepository.findByVendorIdOrderByStartedAtDesc(vendor.getId());
    }

    @Transactional(readOnly = true)
    public List<Vendor> listVendors() {
        return vendorRepository.findAll();
    }

    private List<IngestionRun> runForVendors(List<Vendor> vendors, CatalogSyncMode mode, IngestionRunType runType) {
        List<IngestionRun> runs = new ArrayList<>();
        for (Vendor vendor : vendors) {
            try {
                String raw = mockVendorFeedClient.fetchFeed(vendor);
                runs.add(runIngestion(vendor, raw, mode, runType));
            } catch (Exception e) {
                log.error("Scheduled {} failed for {}", runType, vendor.getCode(), e);
            }
        }
        return runs;
    }

    private IngestionRun syncDirectPull(String vendorCode, CatalogSyncMode mode, IngestionRunType runType) {
        Vendor vendor = getVendor(vendorCode);
        if (vendor.getIngestionChannel() != IngestionChannel.DIRECT_PULL) {
            throw new BusinessException(vendorCode + " uses " + vendor.getIngestionChannel() + ", not DIRECT_PULL");
        }
        String raw = mockVendorFeedClient.fetchFeed(vendor);
        return runIngestion(vendor, raw, mode, runType);
    }

    private IngestionRun runIngestion(Vendor vendor, String rawPayload, CatalogSyncMode mode, IngestionRunType runType) {
        IngestionRun run = IngestionRun.builder()
                .vendor(vendor)
                .status(IngestionRunStatus.RUNNING)
                .runType(runType)
                .startedAt(Instant.now())
                .build();
        run = ingestionRunRepository.save(run);

        int products = 0;
        int created = 0;
        int updated = 0;
        int activities = 0;
        int paxTypes = 0;

        try {
            VendorProductMapper mapper = mapperRegistry.getRequired(vendor.getCode());
            List<CanonicalProduct> canonical = mapper.map(rawPayload);
            products = canonical.size();

            for (CanonicalProduct cp : canonical) {
                UpsertOutcome outcome = upsertProduct(vendor, cp, mode);
                if (outcome.applied()) {
                    if (outcome.created()) {
                        created++;
                    } else {
                        updated++;
                    }
                    activities += outcome.activities();
                    paxTypes += outcome.paxTypes();
                }
            }

            run.setStatus(IngestionRunStatus.SUCCESS);
            run.setProductsReceived(products);
            run.setProductsCreated(created);
            run.setProductsUpdated(updated);
            run.setProductsUpserted(created + updated);
            run.setActivitiesUpserted(activities);
            run.setPaxTypesUpserted(paxTypes);
        } catch (Exception e) {
            log.error("Ingestion failed for {} ({})", vendor.getCode(), runType, e);
            run.setStatus(IngestionRunStatus.FAILED);
            run.setErrorMessage(e.getMessage());
        }
        run.setFinishedAt(Instant.now());
        return ingestionRunRepository.save(run);
    }

    private UpsertOutcome upsertProduct(Vendor vendor, CanonicalProduct cp, CatalogSyncMode mode) {
        boolean exists = attractionRepository
                .findByVendor_IdAndVendorExternalId(vendor.getId(), cp.externalId())
                .isPresent();

        if (mode == CatalogSyncMode.INGEST_ONLY && exists) {
            return UpsertOutcome.skipped();
        }
        if (mode == CatalogSyncMode.UPDATE_ONLY && !exists) {
            return UpsertOutcome.skipped();
        }

        City city = cityRepository.findByCodeIgnoreCase(cp.cityCode())
                .orElseThrow(() -> new BusinessException("Unknown city code in feed: " + cp.cityCode()));
        Category category = resolveCategory(cp.categoryName());

        Attraction attraction = attractionRepository
                .findByVendor_IdAndVendorExternalId(vendor.getId(), cp.externalId())
                .orElse(null);

        boolean created = attraction == null;
        if (created) {
            attraction = Attraction.builder()
                    .title(cp.title())
                    .description(cp.description())
                    .type(cp.type() != null ? cp.type() : AttractionType.TOUR)
                    .price(cp.basePrice())
                    .currency(cp.currency())
                    .durationHours(cp.durationHours())
                    .maxGroupSize(20)
                    .averageRating(cp.rating())
                    .reviewCount(cp.reviewCount())
                    .imageUrl(cp.imageUrl())
                    .active(true)
                    .city(city)
                    .category(category)
                    .vendor(vendor)
                    .vendorExternalId(cp.externalId())
                    .build();
        } else {
            attraction.setTitle(cp.title());
            attraction.setDescription(cp.description());
            attraction.setPrice(cp.basePrice());
            attraction.setImageUrl(cp.imageUrl());
            attraction.setAverageRating(cp.rating());
            attraction.setReviewCount(cp.reviewCount());
            attraction.setActive(true);
        }
        final Attraction savedAttraction = attractionRepository.save(attraction);

        if (created && timeSlotRepository.findByAttractionId(savedAttraction.getId()).isEmpty()) {
            timeSlotRepository.save(TimeSlot.builder()
                    .attraction(savedAttraction)
                    .slotDate(LocalDate.now().plusDays(3))
                    .startTime(LocalTime.of(10, 0))
                    .totalSeats(20)
                    .availableSeats(20)
                    .build());
        }

        int actCount = 0;
        for (CanonicalActivity ca : cp.activities()) {
            vendorActivityRepository.findByAttractionIdAndVendorActivityCode(savedAttraction.getId(), ca.externalCode())
                    .ifPresentOrElse(existing -> {
                        existing.setTitle(ca.title());
                        existing.setDescription(ca.description());
                        existing.setActive(true);
                        vendorActivityRepository.save(existing);
                    }, () -> vendorActivityRepository.save(VendorActivity.builder()
                            .attraction(savedAttraction)
                            .vendorActivityCode(ca.externalCode())
                            .title(ca.title())
                            .description(ca.description())
                            .active(true)
                            .build()));
            actCount++;
        }

        int paxCount = 0;
        for (CanonicalPaxType cpt : cp.paxTypes()) {
            paxTypeRepository.findByAttractionIdAndCodeIgnoreCase(savedAttraction.getId(), cpt.code())
                    .ifPresentOrElse(existing -> {
                        existing.setLabel(cpt.label());
                        existing.setMinAge(cpt.minAge());
                        existing.setMaxAge(cpt.maxAge());
                        existing.setPrice(cpt.price());
                        existing.setCurrency(cpt.currency());
                        existing.setActive(true);
                        paxTypeRepository.save(existing);
                    }, () -> paxTypeRepository.save(PaxType.builder()
                            .attraction(savedAttraction)
                            .code(cpt.code())
                            .label(cpt.label())
                            .minAge(cpt.minAge())
                            .maxAge(cpt.maxAge())
                            .price(cpt.price())
                            .currency(cpt.currency())
                            .active(true)
                            .build()));
            paxCount++;
        }

        return new UpsertOutcome(true, created, actCount, paxCount);
    }

    private Category resolveCategory(String vendorCategory) {
        if (vendorCategory == null || vendorCategory.isBlank()) {
            return categoryRepository.findByNameIgnoreCase("Adventure")
                    .orElseThrow(() -> new BusinessException("Default category missing"));
        }
        return categoryRepository.findByNameIgnoreCase(vendorCategory)
                .or(() -> categoryRepository.findByNameIgnoreCase(
                        switch (vendorCategory.toLowerCase()) {
                            case "culture" -> "Culture & Heritage";
                            case "food" -> "Food & Nightlife";
                            case "attraction" -> "Family Fun";
                            case "adventure" -> "Adventure";
                            default -> "Adventure";
                        }))
                .orElseGet(() -> categoryRepository.findByNameIgnoreCase("Adventure")
                        .orElseThrow(() -> new BusinessException("Default category missing")));
    }

    private Vendor getVendor(String vendorCode) {
        return vendorRepository.findByCodeIgnoreCase(vendorCode)
                .orElseThrow(() -> new ResourceNotFoundException("Vendor not found: " + vendorCode));
    }

    private record UpsertOutcome(boolean applied, boolean created, int activities, int paxTypes) {
        static UpsertOutcome skipped() {
            return new UpsertOutcome(false, false, 0, 0);
        }
    }
}
