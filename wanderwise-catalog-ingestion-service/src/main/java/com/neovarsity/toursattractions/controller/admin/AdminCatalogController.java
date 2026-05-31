package com.neovarsity.toursattractions.controller.admin;

import com.neovarsity.toursattractions.config.AdminSecretGuard;
import com.neovarsity.toursattractions.dto.request.UpdateAttractionRequest;
import com.neovarsity.toursattractions.dto.response.ApiResponse;
import com.neovarsity.toursattractions.dto.response.AttractionResponse;
import com.neovarsity.toursattractions.service.AttractionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/catalog")
@RequiredArgsConstructor
public class AdminCatalogController {

    private final AttractionService attractionService;
    private final AdminSecretGuard adminSecretGuard;

    @GetMapping("/products")
    public ApiResponse<Page<AttractionResponse>> listProducts(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestHeader(value = "X-Admin-Secret", required = false) String adminSecret) {
        adminSecretGuard.requireAdmin(adminSecret);
        return ApiResponse.ok(attractionService.listForAdmin(keyword, PageRequest.of(page, size)));
    }

    @GetMapping("/products/{id}")
    public ApiResponse<AttractionResponse> getProduct(
            @PathVariable Long id,
            @RequestHeader(value = "X-Admin-Secret", required = false) String adminSecret) {
        adminSecretGuard.requireAdmin(adminSecret);
        return ApiResponse.ok(attractionService.getForAdmin(id));
    }

    @PutMapping("/products/{id}")
    public ApiResponse<AttractionResponse> updateProduct(
            @PathVariable Long id,
            @Valid @RequestBody UpdateAttractionRequest request,
            @RequestHeader(value = "X-Admin-Secret", required = false) String adminSecret) {
        adminSecretGuard.requireAdmin(adminSecret);
        return ApiResponse.ok("Product updated", attractionService.update(id, request));
    }
}
