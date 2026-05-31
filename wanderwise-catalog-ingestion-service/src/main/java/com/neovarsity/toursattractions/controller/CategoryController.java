package com.neovarsity.toursattractions.controller;

import com.neovarsity.toursattractions.dto.response.ApiResponse;
import com.neovarsity.toursattractions.entity.Category;
import com.neovarsity.toursattractions.service.CatalogService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CatalogService catalogService;

    @GetMapping
    public ApiResponse<List<Category>> list() {
        return ApiResponse.ok(catalogService.getAllCategories());
    }
}
