package com.neovarsity.toursattractions.service;

import com.neovarsity.toursattractions.dto.response.TimeSlotResponse;
import com.neovarsity.toursattractions.entity.Attraction;
import com.neovarsity.toursattractions.entity.TimeSlot;
import com.neovarsity.toursattractions.exception.BusinessException;
import com.neovarsity.toursattractions.exception.ResourceNotFoundException;
import com.neovarsity.toursattractions.repository.TimeSlotRepository;
import com.neovarsity.toursattractions.util.EntityMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TimeSlotService {

    private final TimeSlotRepository timeSlotRepository;
    private final AttractionService attractionService;

    @Transactional(readOnly = true)
    public List<TimeSlotResponse> listAvailable(Long attractionId) {
        return timeSlotRepository
                .findByAttractionIdAndSlotDateGreaterThanEqualOrderBySlotDateAscStartTimeAsc(
                        attractionId, LocalDate.now())
                .stream()
                .filter(s -> s.getAvailableSeats() > 0)
                .map(EntityMapper::toTimeSlotResponse)
                .toList();
    }

    @Transactional
    public TimeSlot createSlot(Long attractionId, LocalDate date, LocalTime startTime, int totalSeats) {
        Attraction attraction = attractionService.findEntity(attractionId);
        TimeSlot slot = TimeSlot.builder()
                .attraction(attraction)
                .slotDate(date)
                .startTime(startTime)
                .totalSeats(totalSeats)
                .availableSeats(totalSeats)
                .build();
        return timeSlotRepository.save(slot);
    }

    @Transactional
    public void reserveSeats(Long slotId, int guests) {
        TimeSlot slot = timeSlotRepository.findByIdForUpdate(slotId)
                .orElseThrow(() -> new ResourceNotFoundException("Time slot not found: " + slotId));
        if (slot.getAvailableSeats() < guests) {
            throw new BusinessException("Not enough seats available for this slot");
        }
        slot.setAvailableSeats(slot.getAvailableSeats() - guests);
    }

    @Transactional(readOnly = true)
    public TimeSlot findEntity(Long id) {
        return timeSlotRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Time slot not found: " + id));
    }

    @Transactional
    public void releaseSeats(Long slotId, int guests) {
        timeSlotRepository.findById(slotId).ifPresent(slot -> {
            int restored = Math.min(slot.getTotalSeats(), slot.getAvailableSeats() + guests);
            slot.setAvailableSeats(restored);
        });
    }
}
