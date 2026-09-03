package com.ps.cinema_back.seat.service.impl;

import com.ps.cinema_back.common.exception.BadRequestException;
import com.ps.cinema_back.common.exception.ConflictException;
import com.ps.cinema_back.common.exception.ResourceNotFoundException;
import com.ps.cinema_back.hall.entity.Hall;
import com.ps.cinema_back.hall.repository.HallRepository;
import com.ps.cinema_back.seat.dto.request.BatchSeatCreateRequest;
import com.ps.cinema_back.seat.dto.request.BulkSeatGenerateRequest;
import com.ps.cinema_back.seat.dto.request.SeatRequest;
import com.ps.cinema_back.seat.dto.response.SeatResponse;
import com.ps.cinema_back.seat.entity.Seat;
import com.ps.cinema_back.seat.repository.SeatRepository;
import com.ps.cinema_back.seat.service.SeatService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SeatServiceImpl implements SeatService {

    private final SeatRepository seatRepository;
    private final HallRepository hallRepository;

    @Override
    @Transactional
    public SeatResponse createSeat(SeatRequest request) {
        Hall hall = hallRepository.findByIdAndIsDeletedFalse(request.getHallId())
                .orElseThrow(() -> new ResourceNotFoundException("Hall not found with id: " + request.getHallId()));

        String formattedRow = request.getSeatRow().trim().toUpperCase();

        if (seatRepository.existsByHallIdAndSeatRowIgnoreCaseAndSeatNumberAndIsDeletedFalse(
                hall.getId(), formattedRow, request.getSeatNumber())) {
            throw new ConflictException("Seat " + formattedRow + request.getSeatNumber() + " already exists in this hall");
        }

        int fallbackGridY = 90 - (int) formattedRow.charAt(0);

        Seat seat = Seat.builder()
                .seatRow(formattedRow)
                .seatNumber(request.getSeatNumber())
                .seatType(request.getSeatType())
                .gridX(request.getGridX() != null ? request.getGridX() : request.getSeatNumber())
                .gridY(request.getGridY() != null ? request.getGridY() : fallbackGridY)
                .hall(hall)
                .isDeleted(false)
                .build();

        Seat savedSeat = seatRepository.save(seat);
        updateHallTotalSeats(hall);

        return mapToResponse(savedSeat);
    }

    @Override
    @Transactional
    public List<SeatResponse> generateBulkSeats(BulkSeatGenerateRequest request) {
        Hall hall = hallRepository.findByIdAndIsDeletedFalse(request.getHallId())
                .orElseThrow(() -> new ResourceNotFoundException("Hall not found with id: " + request.getHallId()));

        char start = request.getStartRow().trim().toUpperCase().charAt(0);
        char end = request.getEndRow().trim().toUpperCase().charAt(0);

        if (start > end) {
            throw new BadRequestException("Start row cannot be after End row (e.g. Start: A, End: H)");
        }

        List<Seat> seatsToSave = new ArrayList<>();

        for (char row = start; row <= end; row++) {
            String rowStr = String.valueOf(row);
            int gridY = 90 - (int) row;

            for (int num = 1; num <= request.getSeatsPerRow(); num++) {
                if (!seatRepository.existsByHallIdAndSeatRowIgnoreCaseAndSeatNumberAndIsDeletedFalse(hall.getId(), rowStr, num)) {
                    seatsToSave.add(Seat.builder()
                            .seatRow(rowStr)
                            .seatNumber(num)
                            .seatType(request.getSeatType())
                            .gridX(num)
                            .gridY(gridY)
                            .hall(hall)
                            .isDeleted(false)
                            .build());
                }
            }
        }

        List<Seat> savedSeats = seatRepository.saveAll(seatsToSave);
        updateHallTotalSeats(hall);

        return savedSeats.stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public List<SeatResponse> saveBatchSeats(BatchSeatCreateRequest request) {
        Hall hall = hallRepository.findByIdAndIsDeletedFalse(request.getHallId())
                .orElseThrow(() -> new ResourceNotFoundException("Hall not found with id: " + request.getHallId()));

        // 1. Delete all existing seats for this specific hall
        seatRepository.hardDeleteAllByHallId(hall.getId());

        // 2. If clearing to 0 seats (empty request)
        if (request.getSeats() == null || request.getSeats().isEmpty()) {
            hall.setTotalSeats(0);
            hallRepository.save(hall);
            return Collections.emptyList();
        }

        // 3. Otherwise, map and insert new seats
        List<Seat> seatsToSave = request.getSeats().stream().map(sReq -> {
            String row = sReq.getSeatRow().trim().toUpperCase();
            int fallbackGridY = 90 - (int) row.charAt(0);

            return Seat.builder()
                    .seatRow(row)
                    .seatNumber(sReq.getSeatNumber())
                    .seatType(sReq.getSeatType())
                    .gridX(sReq.getGridX() != null ? sReq.getGridX() : sReq.getSeatNumber())
                    .gridY(sReq.getGridY() != null ? sReq.getGridY() : fallbackGridY)
                    .hall(hall)
                    .isDeleted(false)
                    .build();
        }).collect(Collectors.toList());

        List<Seat> savedSeats = seatRepository.saveAll(seatsToSave);

        // Update total seats counter on hall
        hall.setTotalSeats(savedSeats.size());
        hallRepository.save(hall);

        return savedSeats.stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public SeatResponse getSeatById(Long id) {
        Seat seat = seatRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Seat not found with id: " + id));
        return mapToResponse(seat);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SeatResponse> getSeatsByHallId(Long hallId) {
        return seatRepository.findAllByHallIdAndIsDeletedFalseOrderBySeatRowAscSeatNumberAsc(hallId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<SeatResponse> getSeatsByHallIdPage(Long hallId, Pageable pageable) {
        return seatRepository.findAllByHallIdAndIsDeletedFalse(hallId, pageable)
                .map(this::mapToResponse);
    }

    @Override
    @Transactional
    public SeatResponse updateSeat(Long id, SeatRequest request) {
        Seat seat = seatRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Seat not found with id: " + id));

        String formattedRow = request.getSeatRow().trim().toUpperCase();

        seat.setSeatRow(formattedRow);
        seat.setSeatNumber(request.getSeatNumber());
        seat.setSeatType(request.getSeatType());
        if (request.getGridX() != null) seat.setGridX(request.getGridX());
        if (request.getGridY() != null) seat.setGridY(request.getGridY());

        return mapToResponse(seatRepository.save(seat));
    }

    @Override
    @Transactional
    public void softDeleteSeat(Long id) {
        Seat seat = seatRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Seat not found with id: " + id));

        seat.setIsDeleted(true);
        seatRepository.save(seat);
        updateHallTotalSeats(seat.getHall());
    }

    @Override
    @Transactional
    public void hardDeleteSeat(Long id) {
        Seat seat = seatRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Seat not found with id: " + id));

        Hall hall = seat.getHall();
        seatRepository.delete(seat);
        updateHallTotalSeats(hall);
    }

    @Override
    @Transactional
    public void hardDeleteAllSeatsByHallId(Long hallId) {
        Hall hall = hallRepository.findByIdAndIsDeletedFalse(hallId)
                .orElseThrow(() -> new ResourceNotFoundException("Hall not found with id: " + hallId));

        seatRepository.hardDeleteAllByHallId(hallId);
        hall.setTotalSeats(0);
        hallRepository.save(hall);
    }

    @Override
    @Transactional
    public SeatResponse restoreSeat(Long id) {
        Seat seat = seatRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Seat not found with id: " + id));

        if (!Boolean.TRUE.equals(seat.getIsDeleted())) {
            throw new BadRequestException("Seat with id " + id + " is not deleted");
        }

        seat.setIsDeleted(false);
        Seat restoredSeat = seatRepository.save(seat);
        updateHallTotalSeats(restoredSeat.getHall());

        return mapToResponse(restoredSeat);
    }

    private void updateHallTotalSeats(Hall hall) {
        long count = seatRepository.countByHallIdAndIsDeletedFalse(hall.getId());
        hall.setTotalSeats((int) count);
        hallRepository.save(hall);
    }

    private SeatResponse mapToResponse(Seat seat) {
        int fallbackGridY = 90 - (int) seat.getSeatRow().trim().toUpperCase().charAt(0);
        return SeatResponse.builder()
                .id(seat.getId())
                .seatCode(seat.getSeatRow() + seat.getSeatNumber())
                .seatRow(seat.getSeatRow())
                .seatNumber(seat.getSeatNumber())
                .seatType(seat.getSeatType())
                .gridX(seat.getGridX() != null ? seat.getGridX() : seat.getSeatNumber())
                .gridY(seat.getGridY() != null ? seat.getGridY() : fallbackGridY)
                .hallId(seat.getHall().getId())
                .hallName(seat.getHall().getName())
                .createdAt(seat.getCreatedAt())
                .updatedAt(seat.getUpdatedAt())
                .build();
    }
}