package com.ps.cinema_back.favorite.repository;

import com.ps.cinema_back.favorite.entity.Favorite;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FavoriteRepository extends JpaRepository<Favorite, Long> {

    Optional<Favorite> findByUserIdAndMovieId(Long userId, Long movieId);

    Page<Favorite> findAllByUserIdAndIsDeletedFalse(Long userId, Pageable pageable);

    Optional<Favorite> findByUserIdAndMovieIdAndIsDeletedFalse(Long userId, Long movieId);

    boolean existsByUserIdAndMovieIdAndIsDeletedFalse(Long userId, Long movieId);

    // Popularity Tracking for Admin: Returns Object[] where [0] is Movie and [1] is Long (watchlist count)
    @Query("SELECT f.movie, COUNT(f) as count FROM Favorite f WHERE f.isDeleted = false GROUP BY f.movie ORDER BY count DESC")
    Page<Object[]> getMoviePopularityAnalytics(Pageable pageable);
}