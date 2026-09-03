package com.ps.cinema_back.movie.controller;

import com.ps.cinema_back.common.controller.BaseController;
import com.ps.cinema_back.common.enums.Language;
import com.ps.cinema_back.common.enums.MovieStatus;
import com.ps.cinema_back.common.response.ApiResponse;
import com.ps.cinema_back.common.response.PageResponse;
import com.ps.cinema_back.movie.dto.request.MovieRequest;
import com.ps.cinema_back.movie.dto.response.MovieResponse;
import com.ps.cinema_back.movie.service.MovieService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/movies")
@RequiredArgsConstructor
@Tag(name = "Movie Management", description = "Endpoints for managing movies, poster uploads, server-side filtering, and trash bin")
public class MovieController extends BaseController {

    private final MovieService movieService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Create a movie with poster upload")
    public ResponseEntity<ApiResponse<MovieResponse>> createMovie(@Valid @ModelAttribute MovieRequest request) {
        return CREATED(movieService.createMovie(request), "Movie created successfully");
    }

    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Update a movie with optional poster upload")
    public ResponseEntity<ApiResponse<MovieResponse>> updateMovie(
            @PathVariable Long id,
            @Valid @ModelAttribute MovieRequest request) {
        return OK(movieService.updateMovie(id, request), "Movie updated successfully");
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get movie by ID")
    public ResponseEntity<ApiResponse<MovieResponse>> getMovieById(@PathVariable Long id) {
        return OK(movieService.getMovieById(id), "Movie retrieved successfully");
    }

    @GetMapping
    @Operation(summary = "Get paginated active movies with search, status, language, and genre server-side filters")
    public ResponseEntity<ApiResponse<PageResponse<MovieResponse>>> getAllMovies(
            @RequestParam(required = false) MovieStatus status,
            @RequestParam(required = false) Language language,
            @RequestParam(required = false) Long genreId,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction) {

        Sort sort = direction.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageRequest = PageRequest.of(page, size, sort);

        return OK(PageResponse.of(movieService.getAllMovies(status, language, genreId, search, pageRequest)), "Movies fetched successfully");
    }

    @GetMapping("/trash")
    @Operation(summary = "Get paginated deleted movies in trash bin")
    public ResponseEntity<ApiResponse<PageResponse<MovieResponse>>> getTrashMovies(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "updatedAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction) {

        Sort sort = direction.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageRequest = PageRequest.of(page, size, sort);

        return OK(PageResponse.of(movieService.getTrashMovies(search, pageRequest)), "Trash movies fetched successfully");
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Soft delete movie (Move to trash)")
    public ResponseEntity<ApiResponse<Void>> softDeleteMovie(@PathVariable Long id) {
        movieService.softDeleteMovie(id);
        return OK(null, "Movie moved to trash successfully");
    }

    @DeleteMapping("/{id}/hard")
    @Operation(summary = "Permanently delete movie from database")
    public ResponseEntity<ApiResponse<Void>> hardDeleteMovie(@PathVariable Long id) {
        movieService.hardDeleteMovie(id);
        return OK(null, "Movie permanently deleted");
    }

    @PutMapping("/{id}/restore")
    @Operation(summary = "Restore movie from trash")
    public ResponseEntity<ApiResponse<MovieResponse>> restoreMovie(@PathVariable Long id) {
        return OK(movieService.restoreMovie(id), "Movie restored successfully");
    }
}