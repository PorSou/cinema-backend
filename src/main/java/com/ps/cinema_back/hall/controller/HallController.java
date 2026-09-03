package com.ps.cinema_back.hall.controller;

import com.ps.cinema_back.common.controller.BaseController;
import com.ps.cinema_back.common.response.ApiResponse;
import com.ps.cinema_back.common.response.PageResponse;
import com.ps.cinema_back.hall.dto.request.HallRequest;
import com.ps.cinema_back.hall.dto.response.HallResponse;
import com.ps.cinema_back.hall.service.HallService;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/halls")
@RequiredArgsConstructor
public class HallController extends BaseController {

    private final HallService hallService;

    @PostMapping
    public ResponseEntity<ApiResponse<HallResponse>> createHall(@Valid @RequestBody HallRequest request) {
        return CREATED(hallService.createHall(request), "Hall created successfully");
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<HallResponse>> getHallById(@PathVariable Long id) {
        return OK(hallService.getHallById(id), "Hall retrieved successfully");
    }

    @GetMapping("/cinema/{cinemaId}")
    public ResponseEntity<ApiResponse<List<HallResponse>>> getHallsByCinema(@PathVariable Long cinemaId) {
        return OK(hallService.getHallsByCinemaId(cinemaId), "Halls fetched successfully");
    }

    @GetMapping("/cinema/{cinemaId}/trash")
    public ResponseEntity<ApiResponse<List<HallResponse>>> getTrashHallsByCinema(@PathVariable Long cinemaId) {
        return OK(hallService.getTrashHallsByCinemaId(cinemaId), "Trash halls fetched successfully");
    }

    @GetMapping("/cinema/{cinemaId}/page")
    public ResponseEntity<ApiResponse<PageResponse<HallResponse>>> getHallsByCinemaPage(
            @PathVariable Long cinemaId,
            @Parameter(hidden = true) Pageable pageable,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction) {

        Sort sort = direction.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageRequest = PageRequest.of(page, size, sort);

        return OK(PageResponse.of(hallService.getHallsByCinemaIdPage(cinemaId, pageRequest)), "Halls page fetched successfully");
    }

    @GetMapping("/cinema/{cinemaId}/trash/page")
    public ResponseEntity<ApiResponse<PageResponse<HallResponse>>> getTrashHallsByCinemaPage(
            @PathVariable Long cinemaId,
            @Parameter(hidden = true) Pageable pageable,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction) {

        Sort sort = direction.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageRequest = PageRequest.of(page, size, sort);

        return OK(PageResponse.of(hallService.getTrashHallsByCinemaIdPage(cinemaId, pageRequest)), "Trash halls page fetched successfully");
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<HallResponse>> updateHall(
            @PathVariable Long id,
            @Valid @RequestBody HallRequest request) {
        return OK(hallService.updateHall(id, request), "Hall updated successfully");
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteHall(@PathVariable Long id) {
        hallService.softDeleteHall(id);
        return OK(null, "Hall moved to trash successfully");
    }

    @PutMapping("/{id}/restore")
    public ResponseEntity<ApiResponse<HallResponse>> restoreHall(@PathVariable Long id) {
        return OK(hallService.restoreHall(id), "Hall restored successfully");
    }

    @DeleteMapping("/{id}/hard")
    public ResponseEntity<ApiResponse<Void>> hardDeleteHall(@PathVariable Long id) {
        hallService.hardDeleteHall(id);
        return OK(null, "Hall permanently deleted");
    }
}