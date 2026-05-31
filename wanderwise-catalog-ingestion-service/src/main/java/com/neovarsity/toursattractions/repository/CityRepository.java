package com.neovarsity.toursattractions.repository;

import com.neovarsity.toursattractions.entity.City;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CityRepository extends JpaRepository<City, Long> {

    Optional<City> findByNameIgnoreCase(String name);

    Optional<City> findByCodeIgnoreCase(String code);
}
