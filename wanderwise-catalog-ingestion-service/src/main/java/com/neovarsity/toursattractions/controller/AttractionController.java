package com.neovarsity.toursattractions.controller;

import com.neovarsity.toursattractions.dto.request.CreateAttractionRequest;
import com.neovarsity.toursattractions.dto.response.ApiResponse;
import com.neovarsity.toursattractions.dto.response.AttractionResponse;
import com.neovarsity.toursattractions.entity.enums.AttractionType;
import com.neovarsity.toursattractions.service.AttractionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import com.neovarsity.toursattractions.util.AttractionSortBuilder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/attractions")
@RequiredArgsConstructor
public class AttractionController {

    private final AttractionService attractionService;

    @PostMapping
    public ApiResponse<AttractionResponse> create(@Valid @RequestBody CreateAttractionRequest request) {
        return ApiResponse.ok("Attraction created", attractionService.create(request));
    }

    @GetMapping("/{id}")
    public ApiResponse<AttractionResponse> get(@PathVariable Long id) {
        return ApiResponse.ok(attractionService.getById(id));
    }

    @GetMapping
    public ApiResponse<Page<AttractionResponse>> search(
            @RequestParam(required = false) Long cityId,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) AttractionType type,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String scode,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "9") int size,
            @RequestParam(defaultValue = "popularity") String sort,
            @RequestParam(required = false) String direction) {

        Page<AttractionResponse> results = attractionService.search(
                cityId, categoryId, type, keyword, scode,
                PageRequest.of(page, size, AttractionSortBuilder.build(sort, direction)));
        return ApiResponse.ok(results);
    }
}
