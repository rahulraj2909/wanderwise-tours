package com.neovarsity.toursattractions.repository;

import com.neovarsity.toursattractions.entity.BookingPaxLine;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BookingPaxLineRepository extends JpaRepository<BookingPaxLine, Long> {
}
