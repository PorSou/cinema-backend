package com.ps.cinema_back.favorite.service.impl;

import com.ps.cinema_back.common.exception.ResourceNotFoundException;
import com.ps.cinema_back.favorite.dto.response.FavoriteResponse;
import com.ps.cinema_back.favorite.dto.response.MoviePopularityResponse;
import com.ps.cinema_back.favorite.entity.Favorite;
import com.ps.cinema_back.favorite.repository.FavoriteRepository;
import com.ps.cinema_back.favorite.service.FavoriteService;
import com.ps.cinema_back.movie.entity.Movie;
import com.ps.cinema_back.movie.repository.MovieRepository;
import com.ps.cinema_back.user.entity.User;
import com.ps.cinema_back.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FavoriteServiceImpl implements FavoriteService {

    private final FavoriteRepository favoriteRepository;
    private final MovieRepository movieRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public boolean toggleFavorite(Long userId, Long movieId) {
        User user = userRepository.findByIdAndIsDeletedFalse(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        Movie movie = movieRepository.findByIdAndIsDeletedFalse(movieId)
                .orElseThrow(() -> new ResourceNotFoundException("Movie not found"));

        // 🌟 Fix: Find the record whether it was deleted or active
        Favorite existing = favoriteRepository.findByUserIdAndMovieId(userId, movieId).orElse(null);

        if (existing != null) {
            // Toggle the current isDeleted status
            boolean newState = Boolean.TRUE.equals(existing.getIsDeleted()); // If it was true, make it false (active)
            existing.setIsDeleted(!newState);
            favoriteRepository.save(existing);
            return !newState; // Returns true if added, false if removed
        } else {
            // Only create a new row if it has literally never been favorited before
            Favorite favorite = Favorite.builder()
                    .user(user)
                    .movie(movie)
                    .isDeleted(false)
                    .build();
            favoriteRepository.save(favorite);
            return true; // Added to watchlist
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Page<FavoriteResponse> getUserWatchlist(Long userId, Pageable pageable) {
        return favoriteRepository.findAllByUserIdAndIsDeletedFalse(userId, pageable)
                .map(fav -> FavoriteResponse.builder()
                        .id(fav.getId())
                        .movieId(fav.getMovie().getId())
                        .movieTitle(fav.getMovie().getTitle())
                        .moviePosterUrl(fav.getMovie().getPosterUrl())
                        .movieDurationMinutes(fav.getMovie().getDurationMinutes())
                        .createdAt(fav.getCreatedAt())
                        .build());
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isMovieFavorited(Long userId, Long movieId) {
        return favoriteRepository.existsByUserIdAndMovieIdAndIsDeletedFalse(userId, movieId);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<MoviePopularityResponse> getPopularityAnalytics(Pageable pageable) {
        Page<Object[]> results = favoriteRepository.getMoviePopularityAnalytics(pageable);

        List<MoviePopularityResponse> content = results.getContent().stream().map(row -> {
            Movie movie = (Movie) row[0];
            Long count = (Long) row[1];
            return MoviePopularityResponse.builder()
                    .movieId(movie.getId())
                    .movieTitle(movie.getTitle())
                    .moviePosterUrl(movie.getPosterUrl())
                    .watchlistCount(count)
                    .build();
        }).collect(Collectors.toList());

        return new PageImpl<>(content, pageable, results.getTotalElements());
    }
}