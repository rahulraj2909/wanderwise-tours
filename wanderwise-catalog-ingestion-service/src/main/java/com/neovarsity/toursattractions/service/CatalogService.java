package com.neovarsity.toursattractions.service;

import com.neovarsity.toursattractions.entity.Category;
import com.neovarsity.toursattractions.entity.City;
import com.neovarsity.toursattractions.repository.CategoryRepository;
import com.neovarsity.toursattractions.repository.CityRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Cached catalog data (cities, categories) — changes rarely, read very often on homepage.
 */
@Service
@RequiredArgsConstructor
public class CatalogService {

    private final CityRepository cityRepository;
    private final CategoryRepository categoryRepository;

    @Cacheable(value = "cities", key = "'all'")
    @Transactional(readOnly = true)
    public List<City> getAllCities() {
        return cityRepository.findAll();
    }

    @Cacheable(value = "categories", key = "'all'")
    @Transactional(readOnly = true)
    public List<Category> getAllCategories() {
        return categoryRepository.findAll();
    }

    @CacheEvict(value = "cities", allEntries = true)
    public void evictCities() {
        // Called when cities are modified (admin)
    }

    @CacheEvict(value = "categories", allEntries = true)
    public void evictCategories() {
        // Called when categories are modified (admin)
    }
}
