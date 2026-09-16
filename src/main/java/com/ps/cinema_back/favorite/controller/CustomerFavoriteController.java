package com.ps.cinema_back.favorite.controller;

import com.ps.cinema_back.common.controller.BaseController;
import com.ps.cinema_back.common.response.ApiResponse;
import com.ps.cinema_back.common.response.PageResponse;
import com.ps.cinema_back.favorite.dto.response.FavoriteResponse;
import com.ps.cinema_back.favorite.service.FavoriteService;
import com.ps.cinema_back.user.entity.User;
import com.ps.cinema_back.user.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/favorites")
@RequiredArgsConstructor
@Tag(name = "Customer Watchlist / Favorites", description = "Endpoints for managing customer movie watchlists")
public class CustomerFavoriteController extends BaseController {

    private final FavoriteService favoriteService;
    private final UserRepository userRepository;

    @PostMapping("/toggle/{movieId}")
    @Operation(summary = "Toggle movie favorite status (Add or Remove from watchlist)")
    public ResponseEntity<ApiResponse<Boolean>> toggleFavorite(
            Authentication authentication,
            @PathVariable Long movieId) {
        User user = getAuthenticatedUser(authentication);
        boolean isFavorited = favoriteService.toggleFavorite(user.getId(), movieId);
        String message = isFavorited ? "Movie added to watchlist" : "Movie removed from watchlist";
        return OK(isFavorited, message);
    }

    @GetMapping
    @Operation(summary = "Get current user's movie watchlist")
    public ResponseEntity<ApiResponse<PageResponse<FavoriteResponse>>> getUserWatchlist(
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        User user = getAuthenticatedUser(authentication);
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return OK(PageResponse.of(favoriteService.getUserWatchlist(user.getId(), pageable)), "Watchlist fetched successfully");
    }

    @GetMapping("/check/{movieId}")
    @Operation(summary = "Check if a specific movie is in the user's watchlist")
    public ResponseEntity<ApiResponse<Boolean>> checkIsFavorited(
            Authentication authentication,
            @PathVariable Long movieId) {
        User user = getAuthenticatedUser(authentication);
        boolean isFavorited = favoriteService.isMovieFavorited(user.getId(), movieId);
        return OK(isFavorited, "Favorite status checked successfully");
    }

    private User getAuthenticatedUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new RuntimeException("User not authenticated");
        }
        return userRepository.findByEmailAndIsDeletedFalse(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
    }
}