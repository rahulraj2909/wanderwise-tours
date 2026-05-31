package com.neovarsity.toursattractions.service;

import com.neovarsity.toursattractions.dto.response.PaxTypeResponse;
import com.neovarsity.toursattractions.dto.response.VendorActivityResponse;
import com.neovarsity.toursattractions.entity.PaxType;
import com.neovarsity.toursattractions.entity.VendorActivity;
import com.neovarsity.toursattractions.repository.PaxTypeRepository;
import com.neovarsity.toursattractions.repository.VendorActivityRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PaxTypeService {

    private final PaxTypeRepository paxTypeRepository;
    private final VendorActivityRepository vendorActivityRepository;

    @Transactional(readOnly = true)
    public List<PaxTypeResponse> listForAttraction(Long attractionId) {
        return paxTypeRepository.findByAttractionIdAndActiveTrueOrderByCodeAsc(attractionId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<VendorActivityResponse> listActivities(Long attractionId) {
        return vendorActivityRepository.findByAttractionIdAndActiveTrue(attractionId).stream()
                .map(a -> VendorActivityResponse.builder()
                        .id(a.getId())
                        .vendorActivityCode(a.getVendorActivityCode())
                        .title(a.getTitle())
                        .description(a.getDescription())
                        .build())
                .toList();
    }

    private PaxTypeResponse toResponse(PaxType p) {
        return PaxTypeResponse.builder()
                .id(p.getId())
                .code(p.getCode())
                .label(p.getLabel())
                .minAge(p.getMinAge())
                .maxAge(p.getMaxAge())
                .price(p.getPrice())
                .currency(p.getCurrency())
                .build();
    }
}
