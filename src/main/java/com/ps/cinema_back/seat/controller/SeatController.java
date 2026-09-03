package com.ps.cinema_back.seat.controller;

import com.ps.cinema_back.common.controller.BaseController;
import com.ps.cinema_back.common.response.ApiResponse;
import com.ps.cinema_back.common.response.PageResponse;
import com.ps.cinema_back.seat.dto.request.BatchSeatCreateRequest;
import com.ps.cinema_back.seat.dto.request.BulkSeatGenerateRequest;
import com.ps.cinema_back.seat.dto.request.SeatRequest;
import com.ps.cinema_back.seat.dto.response.SeatResponse;
import com.ps.cinema_back.seat.service.SeatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/seats")
@RequiredArgsConstructor
@Tag(name = "Seat Management", description = "Endpoints for managing auditorium seats, batch grid layouts, and preset configurations")
public class SeatController extends BaseController {

    private final SeatService seatService;

    @PostMapping
    @Operation(summary = "Create single seat")
    public ResponseEntity<ApiResponse<SeatResponse>> createSeat(@Valid @RequestBody SeatRequest request) {
        return CREATED(seatService.createSeat(request), "Seat created successfully");
    }

    @PostMapping("/bulk-generate")
    @Operation(summary = "Bulk generate rectangular seat matrix")
    public ResponseEntity<ApiResponse<List<SeatResponse>>> generateBulkSeats(
            @Valid @RequestBody BulkSeatGenerateRequest request) {
        return CREATED(seatService.generateBulkSeats(request), "Bulk seats generated successfully");
    }

    @PostMapping("/batch")
    @Operation(summary = "Save batch layout with custom grid coordinates (X, Y)")
    public ResponseEntity<ApiResponse<List<SeatResponse>>> saveBatchSeats(
            @Valid @RequestBody BatchSeatCreateRequest request) {
        List<SeatResponse> savedSeats = seatService.saveBatchSeats(request);
        String message = (request.getSeats() == null || request.getSeats().isEmpty())
                ? "All seats cleared successfully"
                : "Batch seats deployed successfully (" + savedSeats.size() + " seats)";
        return OK(savedSeats, message);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get seat by ID")
    public ResponseEntity<ApiResponse<SeatResponse>> getSeatById(@PathVariable Long id) {
        return OK(seatService.getSeatById(id), "Seat retrieved successfully");
    }

    @GetMapping("/hall/{hallId}")
    @Operation(summary = "Get all active seats for a specific hall")
    public ResponseEntity<ApiResponse<List<SeatResponse>>> getSeatsByHall(@PathVariable Long hallId) {
        return OK(seatService.getSeatsByHallId(hallId), "Seats for hall fetched successfully");
    }

    @GetMapping("/hall/{hallId}/page")
    @Operation(summary = "Get paginated seats for a hall")
    public ResponseEntity<ApiResponse<PageResponse<SeatResponse>>> getSeatsByHallPage(
            @PathVariable Long hallId,
            @Parameter(hidden = true) Pageable pageable,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "seatRow") String sortBy,
            @RequestParam(defaultValue = "asc") String direction) {

        Sort sort = direction.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageRequest = PageRequest.of(page, size, sort);

        return OK(PageResponse.of(seatService.getSeatsByHallIdPage(hallId, pageRequest)), "Seats page fetched successfully");
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update single seat information and type")
    public ResponseEntity<ApiResponse<SeatResponse>> updateSeat(
            @PathVariable Long id,
            @Valid @RequestBody SeatRequest request) {
        return OK(seatService.updateSeat(id, request), "Seat updated successfully");
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Soft delete seat by ID")
    public ResponseEntity<ApiResponse<Void>> deleteSeat(@PathVariable Long id) {
        seatService.softDeleteSeat(id);
        return OK(null, "Seat soft-deleted successfully");
    }

    @DeleteMapping("/{id}/hard")
    @Operation(summary = "Hard delete single seat by ID")
    public ResponseEntity<ApiResponse<Void>> hardDeleteSeat(@PathVariable Long id) {
        seatService.hardDeleteSeat(id);
        return OK(null, "Seat permanently deleted");
    }

    @DeleteMapping("/hall/{hallId}/clear")
    @Operation(summary = "Hard delete and reset all seats in a hall to 0 seats")
    public ResponseEntity<ApiResponse<Void>> clearAllSeatsByHall(@PathVariable Long hallId) {
        seatService.hardDeleteAllSeatsByHallId(hallId);
        return OK(null, "All seats permanently cleared for hall ID: " + hallId);
    }

    @PutMapping("/{id}/restore")
    @Operation(summary = "Restore soft-deleted seat")
    public ResponseEntity<ApiResponse<SeatResponse>> restoreSeat(@PathVariable Long id) {
        return OK(seatService.restoreSeat(id), "Seat restored successfully");
    }
}