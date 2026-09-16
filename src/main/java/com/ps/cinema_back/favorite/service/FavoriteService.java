package com.ps.cinema_back.favorite.service;

import com.ps.cinema_back.favorite.dto.response.FavoriteResponse;
import com.ps.cinema_back.favorite.dto.response.MoviePopularityResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface FavoriteService {
    boolean toggleFavorite(Long userId, Long movieId);
    Page<FavoriteResponse> getUserWatchlist(Long userId, Pageable pageable);
    boolean isMovieFavorited(Long userId, Long movieId);
    Page<MoviePopularityResponse> getPopularityAnalytics(Pageable pageable);
}