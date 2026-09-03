package com.ps.cinema_back.cinema.controller;

import com.ps.cinema_back.cinema.dto.request.CinemaRequest;
import com.ps.cinema_back.cinema.dto.response.CinemaResponse;
import com.ps.cinema_back.cinema.service.CinemaService;
import com.ps.cinema_back.common.controller.BaseController;
import com.ps.cinema_back.common.response.ApiResponse;
import com.ps.cinema_back.common.response.PageResponse;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/cinemas")
@RequiredArgsConstructor
public class CinemaController extends BaseController {

    private final CinemaService cinemaService;

    @PostMapping
    public ResponseEntity<ApiResponse<CinemaResponse>> createCinema(@Valid @RequestBody CinemaRequest request) {
        return CREATED(cinemaService.createCinema(request), "Cinema created successfully");
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CinemaResponse>> getCinemaById(@PathVariable Long id) {
        return OK(cinemaService.getCinemaById(id), "Cinema retrieved successfully");
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<CinemaResponse>>> getAllCinemas(
            @Parameter(hidden = true) Pageable pageable,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction) {

        Sort sort = direction.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageRequest = PageRequest.of(page, size, sort);

        return OK(PageResponse.of(cinemaService.getAllCinemas(pageRequest)), "Cinemas fetched successfully");
    }

    @GetMapping("/trash")
    public ResponseEntity<ApiResponse<PageResponse<CinemaResponse>>> getTrashCinemas(
            @Parameter(hidden = true) Pageable pageable,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction) {

        Sort sort = direction.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageRequest = PageRequest.of(page, size, sort);

        return OK(PageResponse.of(cinemaService.getTrashCinemas(pageRequest)), "Trash cinemas fetched successfully");
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<CinemaResponse>> updateCinema(
            @PathVariable Long id,
            @Valid @RequestBody CinemaRequest request) {
        return OK(cinemaService.updateCinema(id, request), "Cinema updated successfully");
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> softDeleteCinema(@PathVariable Long id) {
        cinemaService.softDeleteCinema(id);
        return OK(null, "Cinema moved to trash successfully");
    }

    @PutMapping("/{id}/restore")
    public ResponseEntity<ApiResponse<CinemaResponse>> restoreCinema(@PathVariable Long id) {
        return OK(cinemaService.restoreCinema(id), "Cinema restored successfully");
    }

    @DeleteMapping("/{id}/hard")
    public ResponseEntity<ApiResponse<Void>> hardDeleteCinema(@PathVariable Long id) {
        cinemaService.hardDeleteCinema(id);
        return OK(null, "Cinema permanently deleted");
    }
}