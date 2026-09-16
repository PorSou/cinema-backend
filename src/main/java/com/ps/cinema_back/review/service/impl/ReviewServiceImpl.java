package com.ps.cinema_back.review.service.impl;

import com.ps.cinema_back.common.exception.BadRequestException;
import com.ps.cinema_back.common.exception.ResourceNotFoundException;
import com.ps.cinema_back.movie.entity.Movie;
import com.ps.cinema_back.movie.repository.MovieRepository;
import com.ps.cinema_back.review.dto.request.ReviewRequest;
import com.ps.cinema_back.review.dto.response.ReviewResponse;
import com.ps.cinema_back.review.entity.Review;
import com.ps.cinema_back.review.repository.ReviewRepository;
import com.ps.cinema_back.review.service.ReviewService;
import com.ps.cinema_back.user.entity.User;
import com.ps.cinema_back.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ReviewServiceImpl implements ReviewService {

    private final ReviewRepository reviewRepository;
    private final MovieRepository movieRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public ReviewResponse createOrUpdateReview(Long userId, ReviewRequest request) {
        User user = userRepository.findByIdAndIsDeletedFalse(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Movie movie = movieRepository.findByIdAndIsDeletedFalse(request.getMovieId())
                .orElseThrow(() -> new ResourceNotFoundException("Movie not found"));

        Review review = reviewRepository.findByUserIdAndMovieIdAndIsDeletedFalse(userId, request.getMovieId())
                .orElse(null);

        if (review == null) {
            review = Review.builder()
                    .user(user)
                    .movie(movie)
                    .rating(request.getRating())
                    .comment(request.getComment())
                    .build();
        } else {
            review.setRating(request.getRating());
            review.setComment(request.getComment());
            review.setIsHidden(false); // Reset hidden status upon user update if needed
        }

        return mapToResponse(reviewRepository.save(review));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ReviewResponse> getReviewsForMovie(Long movieId, Pageable pageable) {
        return reviewRepository.findAllByMovieIdAndIsDeletedFalseAndIsHiddenFalse(movieId, pageable)
                .map(this::mapToResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ReviewResponse> getAllReviewsForAdmin(Pageable pageable) {
        return reviewRepository.findAllByIsDeletedFalse(pageable)
                .map(this::mapToResponse);
    }

    @Override
    @Transactional
    public ReviewResponse toggleHideReview(Long reviewId) {
        Review review = reviewRepository.findByIdAndIsDeletedFalse(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found"));

        review.setIsHidden(!review.getIsHidden());
        return mapToResponse(reviewRepository.save(review));
    }

    @Override
    @Transactional
    public void deleteReview(Long reviewId) {
        Review review = reviewRepository.findByIdAndIsDeletedFalse(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found"));

        review.setIsDeleted(true);
        reviewRepository.save(review);
    }

    private ReviewResponse mapToResponse(Review review) {
        return ReviewResponse.builder()
                .id(review.getId())
                .userId(review.getUser().getId())
                .userFullName(review.getUser().getFullName())
                .movieId(review.getMovie().getId())
                .movieTitle(review.getMovie().getTitle())
                .rating(review.getRating())
                .comment(review.getComment())
                .isHidden(review.getIsHidden())
                .createdAt(review.getCreatedAt())
                .build();
    }
}