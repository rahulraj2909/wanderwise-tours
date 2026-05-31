package com.neovarsity.toursattractions.controller;

import com.neovarsity.toursattractions.dto.response.ApiResponse;
import com.neovarsity.toursattractions.entity.City;
import com.neovarsity.toursattractions.service.CatalogService;
import lombok.RequiredArgsConstructor;
import com.neovarsity.toursattractions.exception.ResourceNotFoundException;
import com.neovarsity.toursattractions.repository.CityRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/cities")
@RequiredArgsConstructor
public class CityController {

    private final CatalogService catalogService;
    private final CityRepository cityRepository;

    @GetMapping
    public ApiResponse<List<City>> list() {
        return ApiResponse.ok(catalogService.getAllCities());
    }

    @GetMapping("/code/{code}")
    public ApiResponse<City> getByCode(@PathVariable String code) {
        return ApiResponse.ok(cityRepository.findByCodeIgnoreCase(code)
                .orElseThrow(() -> new ResourceNotFoundException("City not found: " + code)));
    }
}
