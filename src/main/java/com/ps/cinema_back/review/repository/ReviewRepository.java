package com.ps.cinema_back.review.repository;

import com.ps.cinema_back.review.entity.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {

    Page<Review> findAllByMovieIdAndIsDeletedFalseAndIsHiddenFalse(Long movieId, Pageable pageable);

    Page<Review> findAllByIsDeletedFalse(Pageable pageable);

    Optional<Review> findByIdAndIsDeletedFalse(Long id);

    Optional<Review> findByUserIdAndMovieIdAndIsDeletedFalse(Long userId, Long movieId);

    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.movie.id = :movieId AND r.isDeleted = false AND r.isHidden = false")
    Double getAverageRatingByMovieId(@Param("movieId") Long movieId);

    @Query("SELECT COUNT(r) FROM Review r WHERE r.movie.id = :movieId AND r.isDeleted = false AND r.isHidden = false")
    Long getReviewCountByMovieId(@Param("movieId") Long movieId);
}