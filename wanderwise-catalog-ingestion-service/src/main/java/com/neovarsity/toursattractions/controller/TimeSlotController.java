package com.neovarsity.toursattractions.controller;

import com.neovarsity.toursattractions.dto.response.ApiResponse;
import com.neovarsity.toursattractions.dto.response.TimeSlotResponse;
import com.neovarsity.toursattractions.service.TimeSlotService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/attractions/{attractionId}/slots")
@RequiredArgsConstructor
public class TimeSlotController {

    private final TimeSlotService timeSlotService;

    @GetMapping
    public ApiResponse<List<TimeSlotResponse>> list(@PathVariable Long attractionId) {
        return ApiResponse.ok(timeSlotService.listAvailable(attractionId));
    }

    @PostMapping
    public ApiResponse<TimeSlotResponse> create(
            @PathVariable Long attractionId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime startTime,
            @RequestParam(defaultValue = "20") int totalSeats) {
        return ApiResponse.ok(
                com.neovarsity.toursattractions.util.EntityMapper.toTimeSlotResponse(
                        timeSlotService.createSlot(attractionId, date, startTime, totalSeats)));
    }
}
