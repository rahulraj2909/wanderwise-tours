package com.neovarsity.toursattractions.repository;

import com.neovarsity.toursattractions.entity.TimeSlot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface TimeSlotRepository extends JpaRepository<TimeSlot, Long> {

    List<TimeSlot> findByAttractionId(Long attractionId);

    List<TimeSlot> findByAttractionIdAndSlotDateGreaterThanEqualOrderBySlotDateAscStartTimeAsc(
            Long attractionId, LocalDate fromDate);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM TimeSlot t WHERE t.id = :id")
    Optional<TimeSlot> findByIdForUpdate(@Param("id") Long id);
}
