package com.ps.cinema_back.review.controller;

import com.ps.cinema_back.common.controller.BaseController;
import com.ps.cinema_back.common.response.ApiResponse;
import com.ps.cinema_back.common.response.PageResponse;
import com.ps.cinema_back.review.dto.request.ReviewRequest;
import com.ps.cinema_back.review.dto.response.ReviewResponse;
import com.ps.cinema_back.review.service.ReviewService;
import com.ps.cinema_back.user.entity.User;
import com.ps.cinema_back.user.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/reviews")
@RequiredArgsConstructor
@Tag(name = "Customer Movie Reviews", description = "Endpoints for user movie ratings and reviews")
public class CustomerReviewController extends BaseController {

    private final ReviewService reviewService;
    private final UserRepository userRepository;

    @PostMapping
    @Operation(summary = "Submit or update a review and rating for a movie")
    public ResponseEntity<ApiResponse<ReviewResponse>> submitReview(
            Authentication authentication,
            @Valid @RequestBody ReviewRequest request) {
        User user = getAuthenticatedUser(authentication);
        return OK(reviewService.createOrUpdateReview(user.getId(), request), "Review submitted successfully");
    }

    @GetMapping("/movie/{movieId}")
    @Operation(summary = "Get all public reviews for a specific movie")
    public ResponseEntity<ApiResponse<PageResponse<ReviewResponse>>> getMovieReviews(
            @PathVariable Long movieId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return OK(PageResponse.of(reviewService.getReviewsForMovie(movieId, pageable)), "Reviews fetched successfully");
    }

    private User getAuthenticatedUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new RuntimeException("User not authenticated");
        }
        return userRepository.findByEmailAndIsDeletedFalse(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
    }
}