package com.neovarsity.toursattractions.service;

import com.neovarsity.toursattractions.dto.request.CreateAttractionRequest;
import com.neovarsity.toursattractions.dto.request.UpdateAttractionRequest;
import com.neovarsity.toursattractions.dto.response.AttractionResponse;
import com.neovarsity.toursattractions.entity.Attraction;
import com.neovarsity.toursattractions.entity.Category;
import com.neovarsity.toursattractions.entity.City;
import com.neovarsity.toursattractions.entity.User;
import com.neovarsity.toursattractions.entity.enums.AttractionType;
import com.neovarsity.toursattractions.exception.ResourceNotFoundException;
import com.neovarsity.toursattractions.repository.AttractionRepository;
import com.neovarsity.toursattractions.repository.CategoryRepository;
import com.neovarsity.toursattractions.repository.CityRepository;
import com.neovarsity.toursattractions.repository.UserRepository;
import com.neovarsity.toursattractions.util.EntityMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class AttractionService {

    private final AttractionRepository attractionRepository;
    private final CityRepository cityRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;

    @Transactional
    @CacheEvict(value = {"attractions", "attractionDetail"}, allEntries = true)
    public AttractionResponse create(CreateAttractionRequest request) {
        City city = cityRepository.findById(request.getCityId())
                .orElseThrow(() -> new ResourceNotFoundException("City not found"));
        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));

        User operator = null;
        if (request.getOperatorId() != null) {
            operator = userRepository.findById(request.getOperatorId())
                    .orElseThrow(() -> new ResourceNotFoundException("Operator not found"));
        }

        Attraction attraction = Attraction.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .type(request.getType())
                .price(request.getPrice())
                .currency(request.getCurrency() != null ? request.getCurrency() : "INR")
                .durationHours(request.getDurationHours())
                .maxGroupSize(request.getMaxGroupSize())
                .imageUrl(request.getImageUrl())
                .averageRating(BigDecimal.ZERO)
                .reviewCount(0)
                .active(true)
                .city(city)
                .category(category)
                .operator(operator)
                .build();

        return EntityMapper.toAttractionResponse(attractionRepository.save(attraction));
    }

    @Cacheable(value = "attractionDetail", key = "#id")
    @Transactional(readOnly = true)
    public AttractionResponse getById(Long id) {
        return EntityMapper.toAttractionResponse(findEntity(id));
    }

    @Cacheable(value = "attractions", key = "#cityId + '-' + #categoryId + '-' + #type + '-' + #keyword + '-' + #scode + '-' + #pageable.pageNumber + '-' + #pageable.pageSize + '-' + #pageable.sort", unless = "#result == null || #result.isEmpty()")
    @Transactional(readOnly = true)
    public Page<AttractionResponse> search(Long cityId, Long categoryId, AttractionType type, String keyword,
                                         String scode, Pageable pageable) {
        Long resolvedCityId = cityId;
        if (resolvedCityId == null && scode != null && !scode.isBlank()) {
            resolvedCityId = cityRepository.findByCodeIgnoreCase(scode.trim())
                    .map(City::getId)
                    .orElseThrow(() -> new ResourceNotFoundException("City not found for code: " + scode));
        }
        return attractionRepository.searchActive(resolvedCityId, categoryId, type, keyword, pageable)
                .map(EntityMapper::toAttractionResponse);
    }

    @Transactional
    @CacheEvict(value = {"attractions", "attractionDetail"}, allEntries = true)
    public AttractionResponse update(Long id, UpdateAttractionRequest request) {
        Attraction attraction = findEntityForAdmin(id);
        City city = cityRepository.findById(request.getCityId())
                .orElseThrow(() -> new ResourceNotFoundException("City not found"));
        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));

        attraction.setTitle(request.getTitle());
        attraction.setDescription(request.getDescription());
        attraction.setType(request.getType());
        attraction.setPrice(request.getPrice());
        if (request.getCurrency() != null) {
            attraction.setCurrency(request.getCurrency());
        }
        attraction.setDurationHours(request.getDurationHours());
        if (request.getMaxGroupSize() != null) {
            attraction.setMaxGroupSize(request.getMaxGroupSize());
        }
        attraction.setImageUrl(request.getImageUrl());
        attraction.setCity(city);
        attraction.setCategory(category);
        if (request.getActive() != null) {
            attraction.setActive(request.getActive());
        }
        return EntityMapper.toAttractionResponse(attractionRepository.save(attraction));
    }

    @Transactional(readOnly = true)
    public Page<AttractionResponse> listForAdmin(String keyword, Pageable pageable) {
        Pageable sorted = pageable.getSort().isSorted() ? pageable
                : org.springframework.data.domain.PageRequest.of(
                        pageable.getPageNumber(), pageable.getPageSize(), Sort.by(Sort.Direction.DESC, "updatedAt"));
        return attractionRepository.searchAdminCatalog(keyword, sorted)
                .map(EntityMapper::toAttractionResponse);
    }

    @Transactional(readOnly = true)
    public AttractionResponse getForAdmin(Long id) {
        return EntityMapper.toAttractionResponse(findEntityForAdmin(id));
    }

    @Transactional(readOnly = true)
    public Attraction findEntity(Long id) {
        return attractionRepository.findById(id)
                .filter(Attraction::getActive)
                .orElseThrow(() -> new ResourceNotFoundException("Attraction not found: " + id));
    }

    @Transactional(readOnly = true)
    public Attraction findEntityForAdmin(Long id) {
        return attractionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Attraction not found: " + id));
    }
}
