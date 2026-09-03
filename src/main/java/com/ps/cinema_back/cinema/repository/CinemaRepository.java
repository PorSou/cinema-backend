package com.ps.cinema_back.cinema.repository;

import com.ps.cinema_back.cinema.entity.Cinema;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CinemaRepository extends JpaRepository<Cinema, Long> {
    Optional<Cinema> findByIdAndIsDeletedFalse(Long id);
    Page<Cinema> findAllByIsDeletedFalse(Pageable pageable);
    Page<Cinema> findAllByIsDeletedTrue(Pageable pageable);
    boolean existsByNameIgnoreCaseAndCityIgnoreCaseAndIsDeletedFalse(String name, String city);
}