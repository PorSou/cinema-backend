package com.ps.cinema_back.genre.controller;

import com.ps.cinema_back.common.controller.BaseController;
import com.ps.cinema_back.common.response.ApiResponse;
import com.ps.cinema_back.common.response.PageResponse;
import com.ps.cinema_back.genre.dto.request.GenreRequest;
import com.ps.cinema_back.genre.dto.response.GenreResponse;
import com.ps.cinema_back.genre.service.GenreService;
import io.swagger.v3.oas.annotations.Operation;
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
@RequestMapping("/api/v1/genres")
@RequiredArgsConstructor
@Tag(name = "Genre Management", description = "Endpoints for managing movie genres, trash bin, and hard deletes")
public class GenreController extends BaseController {

    private final GenreService genreService;

    @PostMapping
    @Operation(summary = "Create a new genre")
    public ResponseEntity<ApiResponse<GenreResponse>> createGenre(@Valid @RequestBody GenreRequest request) {
        return CREATED(genreService.createGenre(request), "Genre created successfully");
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get genre by ID")
    public ResponseEntity<ApiResponse<GenreResponse>> getGenreById(@PathVariable Long id) {
        return OK(genreService.getGenreById(id), "Genre retrieved successfully");
    }

    @GetMapping
    @Operation(summary = "Get paginated active genres")
    public ResponseEntity<ApiResponse<PageResponse<GenreResponse>>> getAllGenres(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction) {

        Sort sort = direction.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageRequest = PageRequest.of(page, size, sort);

        return OK(PageResponse.of(genreService.getAllGenres(pageRequest)), "Genres fetched successfully");
    }

    @GetMapping("/list")
    @Operation(summary = "Get full active genres list for dropdowns")
    public ResponseEntity<ApiResponse<List<GenreResponse>>> getAllActiveGenresList() {
        return OK(genreService.getAllActiveGenresList(), "Active genres list fetched successfully");
    }

    @GetMapping("/trash")
    @Operation(summary = "Get paginated deleted genres in trash")
    public ResponseEntity<ApiResponse<PageResponse<GenreResponse>>> getTrashGenres(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "updatedAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction) {

        Sort sort = direction.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageRequest = PageRequest.of(page, size, sort);

        return OK(PageResponse.of(genreService.getTrashGenres(pageRequest)), "Trash genres fetched successfully");
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update genre")
    public ResponseEntity<ApiResponse<GenreResponse>> updateGenre(
            @PathVariable Long id,
            @Valid @RequestBody GenreRequest request) {
        return OK(genreService.updateGenre(id, request), "Genre updated successfully");
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Soft delete genre (Move to trash)")
    public ResponseEntity<ApiResponse<Void>> softDeleteGenre(@PathVariable Long id) {
        genreService.softDeleteGenre(id);
        return OK(null, "Genre moved to trash successfully");
    }

    @DeleteMapping("/{id}/hard")
    @Operation(summary = "Permanently delete genre")
    public ResponseEntity<ApiResponse<Void>> hardDeleteGenre(@PathVariable Long id) {
        genreService.hardDeleteGenre(id);
        return OK(null, "Genre permanently deleted");
    }

    @PutMapping("/{id}/restore")
    @Operation(summary = "Restore genre from trash")
    public ResponseEntity<ApiResponse<GenreResponse>> restoreGenre(@PathVariable Long id) {
        return OK(genreService.restoreGenre(id), "Genre restored successfully");
    }
}