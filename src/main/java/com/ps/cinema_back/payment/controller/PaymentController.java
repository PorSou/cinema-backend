package com.ps.cinema_back.payment.controller;

import com.ps.cinema_back.common.controller.BaseController;
import com.ps.cinema_back.common.exception.ResourceNotFoundException;
import com.ps.cinema_back.common.exception.UnauthorizedException;
import com.ps.cinema_back.common.response.ApiResponse;
import com.ps.cinema_back.common.response.PageResponse;
import com.ps.cinema_back.payment.dto.request.KhqrGenerateRequest;
import com.ps.cinema_back.payment.dto.response.PaymentResponse;
import com.ps.cinema_back.payment.service.PaymentService;
import com.ps.cinema_back.user.entity.User;
import com.ps.cinema_back.user.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
@Tag(name = "Payment & Bakong KHQR Controller", description = "Endpoints for KHQR payment generation, verification, and receipts")
public class PaymentController extends BaseController {

    private final PaymentService paymentService;
    private final UserRepository userRepository;

    @PostMapping("/khqr/generate")
    @Operation(summary = "Generate Bakong KHQR code for a pending booking")
    public ResponseEntity<ApiResponse<PaymentResponse>> generateKhqr(
            @Parameter(hidden = true) Authentication authentication,
            @Valid @RequestBody KhqrGenerateRequest request) {

        User currentUser = getAuthenticatedUser(authentication);
        return OK(paymentService.generateKhqr(currentUser.getId(), request), "Bakong KHQR generated successfully");
    }

    // Added to match frontend polling request: POST /api/v1/payments/khqr/check-md5
    @PostMapping("/khqr/check-md5")
    @Operation(summary = "Verify Bakong transaction by checking MD5 status")
    public ResponseEntity<ApiResponse<PaymentResponse>> checkBakongMd5(
            @RequestBody Map<String, String> payload) {

        String transactionId = payload.get("transactionId");
        if (transactionId == null || transactionId.isBlank()) {
            // Fallback if transactionId is passed directly as a string or different key
            transactionId = payload.get("md5") != null ? payload.get("md5") : "";
        }

        return OK(paymentService.verifyBakongPayment(transactionId), "Payment verified successfully");
    }

    @PostMapping("/verify-bakong/{transactionId}")
    @Operation(summary = "Verify Bakong transaction callback and confirm booking")
    public ResponseEntity<ApiResponse<PaymentResponse>> verifyBakongPayment(@PathVariable String transactionId) {
        return OK(paymentService.verifyBakongPayment(transactionId), "Payment verified and booking confirmed successfully");
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get payment receipt by ID")
    public ResponseEntity<ApiResponse<PaymentResponse>> getPaymentById(@PathVariable Long id) {
        return OK(paymentService.getPaymentById(id), "Payment retrieved successfully");
    }

    @GetMapping("/transaction/{transactionId}")
    @Operation(summary = "Get payment details by Bakong Transaction ID")
    public ResponseEntity<ApiResponse<PaymentResponse>> getPaymentByTransactionId(@PathVariable String transactionId) {
        return OK(paymentService.getPaymentByTransactionId(transactionId), "Payment retrieved successfully");
    }

    @GetMapping("/booking/{bookingId}")
    @Operation(summary = "Get payment receipt by Booking ID")
    public ResponseEntity<ApiResponse<PaymentResponse>> getPaymentByBookingId(@PathVariable Long bookingId) {
        return OK(paymentService.getPaymentByBookingId(bookingId), "Payment retrieved successfully");
    }

    @GetMapping
    @Operation(summary = "Get all payments (Admin/Staff only)")
    public ResponseEntity<ApiResponse<PageResponse<PaymentResponse>>> getAllPayments(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction) {

        Sort sort = direction.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageRequest = PageRequest.of(page, size, sort);

        return OK(PageResponse.of(paymentService.getAllPayments(pageRequest)), "All payments fetched successfully");
    }

    @PutMapping("/{id}/refund")
    @Operation(summary = "Refund a payment and cancel booking (Admin only)")
    public ResponseEntity<ApiResponse<PaymentResponse>> refundPayment(@PathVariable Long id) {
        return OK(paymentService.refundPayment(id), "Payment refunded and booking cancelled successfully");
    }

    private User getAuthenticatedUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new UnauthorizedException("User is not authenticated");
        }
        return userRepository.findByEmailAndIsDeletedFalse(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("Authenticated user not found"));
    }
}