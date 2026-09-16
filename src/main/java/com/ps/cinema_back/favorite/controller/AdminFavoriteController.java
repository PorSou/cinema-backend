package com.ps.cinema_back.favorite.controller;

import com.ps.cinema_back.common.controller.BaseController;
import com.ps.cinema_back.common.response.ApiResponse;
import com.ps.cinema_back.common.response.PageResponse;
import com.ps.cinema_back.favorite.dto.response.MoviePopularityResponse;
import com.ps.cinema_back.favorite.service.FavoriteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/analytics/popularity")
@RequiredArgsConstructor
@Tag(name = "Admin Movie Popularity Analytics", description = "Endpoints for analyzing movie demand via watchlists")
@PreAuthorize("hasRole('ADMIN')")
public class AdminFavoriteController extends BaseController {

    private final FavoriteService favoriteService;

    @GetMapping
    @Operation(summary = "Get movie demand ranking based on total user watchlists")
    public ResponseEntity<ApiResponse<PageResponse<MoviePopularityResponse>>> getMoviePopularityAnalytics(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return OK(PageResponse.of(favoriteService.getPopularityAnalytics(pageable)), "Movie popularity metrics fetched successfully");
    }
}