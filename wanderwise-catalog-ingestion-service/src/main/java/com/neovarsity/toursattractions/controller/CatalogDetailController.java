package com.neovarsity.toursattractions.controller;

import com.neovarsity.toursattractions.dto.response.ApiResponse;
import com.neovarsity.toursattractions.dto.response.PaxTypeResponse;
import com.neovarsity.toursattractions.dto.response.VendorActivityResponse;
import com.neovarsity.toursattractions.service.PaxTypeService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/attractions/{attractionId}")
@RequiredArgsConstructor
public class CatalogDetailController {

    private final PaxTypeService paxTypeService;

    @GetMapping("/pax-types")
    public ApiResponse<List<PaxTypeResponse>> paxTypes(@PathVariable Long attractionId) {
        return ApiResponse.ok(paxTypeService.listForAttraction(attractionId));
    }

    @GetMapping("/activities")
    public ApiResponse<List<VendorActivityResponse>> activities(@PathVariable Long attractionId) {
        return ApiResponse.ok(paxTypeService.listActivities(attractionId));
    }
}
