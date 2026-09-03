package com.ps.cinema_back.seat.repository;

import com.ps.cinema_back.seat.entity.Seat;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SeatRepository extends JpaRepository<Seat, Long> {

    Optional<Seat> findByIdAndIsDeletedFalse(Long id);

    List<Seat> findAllByHallIdAndIsDeletedFalseOrderBySeatRowAscSeatNumberAsc(Long hallId);

    Page<Seat> findAllByHallIdAndIsDeletedFalse(Long hallId, Pageable pageable);

    boolean existsByHallIdAndSeatRowIgnoreCaseAndSeatNumberAndIsDeletedFalse(Long hallId, String seatRow, Integer seatNumber);

    long countByHallIdAndIsDeletedFalse(Long hallId);

    @Modifying
    @Query("DELETE FROM Seat s WHERE s.hall.id = :hallId")
    void hardDeleteAllByHallId(@Param("hallId") Long hallId);
}