package com.ps.cinema_back.hall.repository;

import com.ps.cinema_back.hall.entity.Hall;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface HallRepository extends JpaRepository<Hall, Long> {
    Optional<Hall> findByIdAndIsDeletedFalse(Long id);
    Page<Hall> findAllByCinemaIdAndIsDeletedFalse(Long cinemaId, Pageable pageable);
    List<Hall> findAllByCinemaIdAndIsDeletedFalse(Long cinemaId);
    Page<Hall> findAllByCinemaIdAndIsDeletedTrue(Long cinemaId, Pageable pageable);
    List<Hall> findAllByCinemaIdAndIsDeletedTrue(Long cinemaId);
    boolean existsByNameIgnoreCaseAndCinemaIdAndIsDeletedFalse(String name, Long cinemaId);
}