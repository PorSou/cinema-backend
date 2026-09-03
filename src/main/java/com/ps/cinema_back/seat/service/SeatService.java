package com.ps.cinema_back.seat.service;

import com.ps.cinema_back.seat.dto.request.BatchSeatCreateRequest;
import com.ps.cinema_back.seat.dto.request.BulkSeatGenerateRequest;
import com.ps.cinema_back.seat.dto.request.SeatRequest;
import com.ps.cinema_back.seat.dto.response.SeatResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface SeatService {

    SeatResponse createSeat(SeatRequest request);

    List<SeatResponse> generateBulkSeats(BulkSeatGenerateRequest request);

    List<SeatResponse> saveBatchSeats(BatchSeatCreateRequest request);

    SeatResponse getSeatById(Long id);

    List<SeatResponse> getSeatsByHallId(Long hallId);

    Page<SeatResponse> getSeatsByHallIdPage(Long hallId, Pageable pageable);

    SeatResponse updateSeat(Long id, SeatRequest request);

    void softDeleteSeat(Long id);

    void hardDeleteSeat(Long id);

    void hardDeleteAllSeatsByHallId(Long hallId);

    SeatResponse restoreSeat(Long id);
}