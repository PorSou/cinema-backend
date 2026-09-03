package com.ps.cinema_back.showtime.controller;

import com.ps.cinema_back.common.controller.BaseController;
import com.ps.cinema_back.common.response.ApiResponse;
import com.ps.cinema_back.common.response.PageResponse;
import com.ps.cinema_back.showtime.dto.request.ShowtimeRequest;
import com.ps.cinema_back.showtime.dto.response.ShowtimeResponse;
import com.ps.cinema_back.showtime.service.ShowtimeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/showtimes")
@RequiredArgsConstructor
public class ShowtimeController extends BaseController {

    private final ShowtimeService showtimeService;

    @PostMapping
    public ResponseEntity<ApiResponse<ShowtimeResponse>> createShowtime(@Valid @RequestBody ShowtimeRequest request) {
        return CREATED(showtimeService.createShowtime(request), "Showtime scheduled successfully");
    }

    @PostMapping("/batch")
    public ResponseEntity<ApiResponse<List<ShowtimeResponse>>> createBatchShowtimes(
            @Valid @RequestBody List<ShowtimeRequest> requests) {
        return CREATED(showtimeService.createBatchShowtimes(requests), "Batch showtimes scheduled successfully");
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ShowtimeResponse>> getShowtimeById(@PathVariable Long id) {
        return OK(showtimeService.getShowtimeById(id), "Showtime retrieved successfully");
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<ShowtimeResponse>>> getAllShowtimes(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "startTime") String sortBy,
            @RequestParam(defaultValue = "asc") String direction) {

        Sort sort = direction.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageRequest = PageRequest.of(page, size, sort);

        return OK(PageResponse.of(showtimeService.getAllShowtimes(pageRequest)), "Showtimes fetched successfully");
    }

    @GetMapping("/trash")
    public ResponseEntity<ApiResponse<PageResponse<ShowtimeResponse>>> getTrashShowtimes(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("updatedAt").descending());
        return OK(PageResponse.of(showtimeService.getTrashShowtimes(pageable)), "Trash showtimes fetched successfully");
    }

    @GetMapping("/movie/{movieId}")
    public ResponseEntity<ApiResponse<List<ShowtimeResponse>>> getShowtimesByMovie(@PathVariable Long movieId) {
        return OK(showtimeService.getShowtimesByMovie(movieId), "Movie showtimes fetched successfully");
    }

    @GetMapping("/movie/{movieId}/date")
    public ResponseEntity<ApiResponse<List<ShowtimeResponse>>> getShowtimesByMovieAndDate(
            @PathVariable Long movieId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return OK(showtimeService.getShowtimesByMovieAndDate(movieId, date), "Showtimes for selected date fetched successfully");
    }

    @GetMapping("/hall/{hallId}")
    public ResponseEntity<ApiResponse<List<ShowtimeResponse>>> getShowtimesByHall(@PathVariable Long hallId) {
        return OK(showtimeService.getShowtimesByHall(hallId), "Hall showtimes fetched successfully");
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ShowtimeResponse>> updateShowtime(
            @PathVariable Long id,
            @Valid @RequestBody ShowtimeRequest request) {
        return OK(showtimeService.updateShowtime(id, request), "Showtime updated successfully");
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> softDeleteShowtime(@PathVariable Long id) {
        showtimeService.softDeleteShowtime(id);
        return OK(null, "Showtime moved to trash successfully");
    }

    @DeleteMapping("/{id}/hard")
    public ResponseEntity<ApiResponse<Void>> hardDeleteShowtime(@PathVariable Long id) {
        showtimeService.hardDeleteShowtime(id);
        return OK(null, "Showtime permanently deleted");
    }

    @PutMapping("/{id}/restore")
    public ResponseEntity<ApiResponse<ShowtimeResponse>> restoreShowtime(@PathVariable Long id) {
        return OK(showtimeService.restoreShowtime(id), "Showtime restored successfully");
    }
}