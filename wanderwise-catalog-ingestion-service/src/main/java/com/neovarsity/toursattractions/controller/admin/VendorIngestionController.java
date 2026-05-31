package com.neovarsity.toursattractions.controller.admin;

import com.neovarsity.toursattractions.config.AdminSecretGuard;
import com.neovarsity.toursattractions.dto.response.ApiResponse;
import com.neovarsity.toursattractions.dto.response.IngestionRunResponse;
import com.neovarsity.toursattractions.dto.response.VendorResponse;
import com.neovarsity.toursattractions.entity.IngestionRun;
import com.neovarsity.toursattractions.entity.Vendor;
import com.neovarsity.toursattractions.service.VendorIngestionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/ingestion")
@RequiredArgsConstructor
public class VendorIngestionController {

    private final VendorIngestionService vendorIngestionService;
    private final AdminSecretGuard adminSecretGuard;

    @GetMapping("/vendors")
    public ApiResponse<List<VendorResponse>> listVendors(
            @RequestHeader(value = "X-Admin-Secret", required = false) String adminSecret) {
        adminSecretGuard.requireAdmin(adminSecret);
        return ApiResponse.ok(vendorIngestionService.listVendors().stream().map(this::toVendor).toList());
    }

    /** Full pull: ingest new + update existing */
    @PostMapping("/vendors/{code}/pull")
    public ApiResponse<IngestionRunResponse> directPull(
            @PathVariable String code,
            @RequestHeader(value = "X-Admin-Secret", required = false) String adminSecret) {
        adminSecretGuard.requireAdmin(adminSecret);
        return ApiResponse.ok("Full catalog sync completed", toRun(vendorIngestionService.ingestDirectPull(code)));
    }

    /** Ingest only — new products from feed */
    @PostMapping("/vendors/{code}/ingest")
    public ApiResponse<IngestionRunResponse> ingestNew(
            @PathVariable String code,
            @RequestHeader(value = "X-Admin-Secret", required = false) String adminSecret) {
        adminSecretGuard.requireAdmin(adminSecret);
        return ApiResponse.ok("Catalog ingest (new only) completed", toRun(vendorIngestionService.ingestNewProducts(code)));
    }

    /** Update only — refresh existing products */
    @PostMapping("/vendors/{code}/update")
    public ApiResponse<IngestionRunResponse> updateExisting(
            @PathVariable String code,
            @RequestHeader(value = "X-Admin-Secret", required = false) String adminSecret) {
        adminSecretGuard.requireAdmin(adminSecret);
        return ApiResponse.ok("Catalog update completed", toRun(vendorIngestionService.updateExistingProducts(code)));
    }

    @PostMapping("/vendors/{code}/simulate-kafka")
    public ApiResponse<String> simulateKafka(
            @PathVariable String code,
            @RequestHeader(value = "X-Admin-Secret", required = false) String adminSecret) {
        adminSecretGuard.requireAdmin(adminSecret);
        vendorIngestionService.publishSimulatedKafkaEvent(code);
        return ApiResponse.ok("Kafka event published (async ingestion started)", "accepted");
    }

    @GetMapping("/vendors/{code}/runs")
    public ApiResponse<List<IngestionRunResponse>> listRuns(
            @PathVariable String code,
            @RequestHeader(value = "X-Admin-Secret", required = false) String adminSecret) {
        adminSecretGuard.requireAdmin(adminSecret);
        return ApiResponse.ok(vendorIngestionService.listRuns(code).stream().map(this::toRun).toList());
    }

    private VendorResponse toVendor(Vendor v) {
        return VendorResponse.builder()
                .id(v.getId())
                .code(v.getCode())
                .name(v.getName())
                .ingestionChannel(v.getIngestionChannel())
                .feedLocation(v.getFeedLocation())
                .active(v.getActive())
                .build();
    }

    private IngestionRunResponse toRun(IngestionRun r) {
        return IngestionRunResponse.builder()
                .id(r.getId())
                .vendorCode(r.getVendor().getCode())
                .runType(r.getRunType())
                .status(r.getStatus())
                .productsReceived(r.getProductsReceived())
                .productsCreated(r.getProductsCreated())
                .productsUpdated(r.getProductsUpdated())
                .productsUpserted(r.getProductsUpserted())
                .activitiesUpserted(r.getActivitiesUpserted())
                .paxTypesUpserted(r.getPaxTypesUpserted())
                .errorMessage(r.getErrorMessage())
                .startedAt(r.getStartedAt())
                .finishedAt(r.getFinishedAt())
                .build();
    }
}
