package com.neovarsity.toursattractions.controller.internal;

import com.neovarsity.toursattractions.entity.Attraction;
import com.neovarsity.toursattractions.entity.PaxType;
import com.neovarsity.toursattractions.exception.BusinessException;
import com.neovarsity.toursattractions.exception.ResourceNotFoundException;
import com.neovarsity.toursattractions.repository.PaxTypeRepository;
import com.neovarsity.toursattractions.service.AttractionService;
import com.neovarsity.toursattractions.service.TimeSlotService;
import com.neovarsity.wanderwise.common.dto.ApiResponse;
import com.neovarsity.wanderwise.common.dto.CatalogAttractionDto;
import com.neovarsity.wanderwise.common.dto.CatalogPaxTypeDto;
import com.neovarsity.wanderwise.common.dto.ReserveSeatsRequest;
import com.neovarsity.wanderwise.common.dto.SlotActionResultDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/internal/v1")
@RequiredArgsConstructor
public class InternalCatalogController {

    private final AttractionService attractionService;
    private final TimeSlotService timeSlotService;
    private final PaxTypeRepository paxTypeRepository;

    @Value("${wanderwise.internal-secret:wanderwise-internal}")
    private String internalSecret;

    @GetMapping("/attractions/{id}")
    public ApiResponse<CatalogAttractionDto> getAttraction(
            @PathVariable Long id,
            @RequestHeader(value = "X-Internal-Secret", required = false) String secret) {
        verifySecret(secret);
        Attraction a = attractionService.findEntity(id);
        return ApiResponse.ok(CatalogAttractionDto.builder()
                .id(a.getId())
                .title(a.getTitle())
                .price(a.getPrice())
                .currency(a.getCurrency())
                .maxGroupSize(a.getMaxGroupSize())
                .active(a.getActive())
                .build());
    }

    @GetMapping("/attractions/{id}/pax-types")
    public ApiResponse<List<CatalogPaxTypeDto>> listPaxTypes(
            @PathVariable Long id,
            @RequestHeader(value = "X-Internal-Secret", required = false) String secret) {
        verifySecret(secret);
        attractionService.findEntity(id);
        List<CatalogPaxTypeDto> types = paxTypeRepository.findByAttractionIdAndActiveTrueOrderByCodeAsc(id).stream()
                .map(this::toPaxDto)
                .toList();
        return ApiResponse.ok(types);
    }

    @GetMapping("/pax-types/{paxTypeId}")
    public ApiResponse<CatalogPaxTypeDto> getPaxType(
            @PathVariable Long paxTypeId,
            @RequestHeader(value = "X-Internal-Secret", required = false) String secret) {
        verifySecret(secret);
        PaxType pax = paxTypeRepository.findById(paxTypeId)
                .orElseThrow(() -> new ResourceNotFoundException("Pax type not found: " + paxTypeId));
        return ApiResponse.ok(toPaxDto(pax));
    }

    @PostMapping("/time-slots/{slotId}/reserve")
    public ApiResponse<SlotActionResultDto> reserve(
            @PathVariable Long slotId,
            @Valid @RequestBody ReserveSeatsRequest request,
            @RequestHeader(value = "X-Internal-Secret", required = false) String secret) {
        verifySecret(secret);
        try {
            timeSlotService.reserveSeats(slotId, request.getGuests());
            var slot = timeSlotService.findEntity(slotId);
            return ApiResponse.ok(SlotActionResultDto.builder()
                    .timeSlotId(slotId)
                    .availableSeats(slot.getAvailableSeats())
                    .success(true)
                    .message("Seats reserved")
                    .build());
        } catch (BusinessException ex) {
            return ApiResponse.ok(SlotActionResultDto.builder()
                    .timeSlotId(slotId)
                    .success(false)
                    .message(ex.getMessage())
                    .build());
        }
    }

    @PostMapping("/time-slots/{slotId}/release")
    public ApiResponse<SlotActionResultDto> release(
            @PathVariable Long slotId,
            @Valid @RequestBody ReserveSeatsRequest request,
            @RequestHeader(value = "X-Internal-Secret", required = false) String secret) {
        verifySecret(secret);
        timeSlotService.releaseSeats(slotId, request.getGuests());
        var slot = timeSlotService.findEntity(slotId);
        return ApiResponse.ok(SlotActionResultDto.builder()
                .timeSlotId(slotId)
                .availableSeats(slot.getAvailableSeats())
                .success(true)
                .message("Seats released")
                .build());
    }

    private CatalogPaxTypeDto toPaxDto(PaxType pax) {
        return CatalogPaxTypeDto.builder()
                .id(pax.getId())
                .code(pax.getCode())
                .label(pax.getLabel())
                .price(pax.getPrice())
                .currency(pax.getCurrency())
                .build();
    }

    private void verifySecret(String secret) {
        if (secret == null || !internalSecret.equals(secret)) {
            throw new BusinessException("Invalid internal secret");
        }
    }
}
