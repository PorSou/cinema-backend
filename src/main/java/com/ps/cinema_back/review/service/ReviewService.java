package com.ps.cinema_back.review.service;

import com.ps.cinema_back.review.dto.request.ReviewRequest;
import com.ps.cinema_back.review.dto.response.ReviewResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ReviewService {

    ReviewResponse createOrUpdateReview(Long userId, ReviewRequest request);

    Page<ReviewResponse> getReviewsForMovie(Long movieId, Pageable pageable);

    Page<ReviewResponse> getAllReviewsForAdmin(Pageable pageable);

    ReviewResponse toggleHideReview(Long reviewId);

    void deleteReview(Long reviewId);
}