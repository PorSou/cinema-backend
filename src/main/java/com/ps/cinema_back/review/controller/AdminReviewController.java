package com.ps.cinema_back.review.controller;

import com.ps.cinema_back.common.controller.BaseController;
import com.ps.cinema_back.common.response.ApiResponse;
import com.ps.cinema_back.common.response.PageResponse;
import com.ps.cinema_back.review.dto.response.ReviewResponse;
import com.ps.cinema_back.review.service.ReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/reviews")
@RequiredArgsConstructor
@Tag(name = "Admin Movie Reviews", description = "Admin moderation endpoints for movie reviews")
@PreAuthorize("hasRole('ADMIN')")
public class AdminReviewController extends BaseController {

    private final ReviewService reviewService;

    @GetMapping
    @Operation(summary = "Get all reviews (including hidden/flagged) for moderation")
    public ResponseEntity<ApiResponse<PageResponse<ReviewResponse>>> getAllReviewsForAdmin(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return OK(PageResponse.of(reviewService.getAllReviewsForAdmin(pageable)), "All reviews fetched for moderation");
    }

    @PatchMapping("/{id}/toggle-hide")
    @Operation(summary = "Hide or unhide an inappropriate/spam review")
    public ResponseEntity<ApiResponse<ReviewResponse>> toggleHideReview(@PathVariable Long id) {
        return OK(reviewService.toggleHideReview(id), "Review visibility updated successfully");
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete an inappropriate review")
    public ResponseEntity<ApiResponse<Void>> deleteReview(@PathVariable Long id) {
        reviewService.deleteReview(id);
        return OK(null, "Review deleted successfully");
    }
}