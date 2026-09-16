package com.ps.cinema_back.payment.controller;

import com.ps.cinema_back.common.controller.BaseController;
import com.ps.cinema_back.common.exception.ResourceNotFoundException;
import com.ps.cinema_back.common.exception.UnauthorizedException;
import com.ps.cinema_back.common.response.ApiResponse;
import com.ps.cinema_back.common.response.PageResponse;
import com.ps.cinema_back.payment.dto.request.CashBookingRequest;
import com.ps.cinema_back.payment.dto.request.KhqrGenerateRequest;
import com.ps.cinema_back.payment.dto.request.PaymentRequest;
import com.ps.cinema_back.payment.dto.response.BakongCheckMd5Response;
import com.ps.cinema_back.payment.dto.response.PaymentResponse;
import com.ps.cinema_back.payment.service.PaymentService;
import com.ps.cinema_back.telegrambot.service.TelegramService;
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
    private final TelegramService telegramService;

    @PostMapping("/khqr/check-md5")
    @Operation(summary = "Poll Bakong transaction status by MD5 (non-mutating check)")
    public ResponseEntity<ApiResponse<BakongCheckMd5Response>> checkBakongMd5(
            @RequestBody Map<String, String> payload) {

        String transactionId = payload.get("transactionId");
        if (transactionId == null || transactionId.isBlank()) {
            transactionId = payload.get("md5") != null ? payload.get("md5") : "";
        }

        return OK(paymentService.checkPaymentStatus(transactionId), "Status checked");
    }

    // The atomic create-booking + confirm-cash-payment endpoint your seat
    // page's confirmation modal calls. This is the ONLY place a voucher
    // discount is currently supported, per your request — every other
    // flow below is unchanged.
    @PostMapping("/cash-booking")
    @Operation(summary = "Create booking and confirm Counter Cash payment atomically")
    public ResponseEntity<ApiResponse<PaymentResponse>> bookAndPayCash(
            @Parameter(hidden = true) Authentication authentication,
            @Valid @RequestBody CashBookingRequest request) {

        User currentUser = getAuthenticatedUser(authentication);
        PaymentResponse response = paymentService.bookAndPayCash(currentUser.getId(), request);

        try {
            // 👇 NEW: pass the discount + voucher code through so the
            // Telegram message shows the same Subtotal / Voucher Discount /
            // Total breakdown the customer saw on the confirmation screen.
            telegramService.sendBookingSuccessNotification(
                    response.getBookingNumber(),
                    response.getMovieTitle(),
                    response.getCinemaName(),
                    response.getHallName(),
                    response.getSeatDetails(),
                    response.getConcessionDetails(),
                    response.getAmount().doubleValue(),
                    response.getDiscountAmount() != null ? response.getDiscountAmount().doubleValue() : null,
                    response.getVoucherCode()
            );
        } catch (Exception e) {
            // booking/payment still succeeds even if telegram fails
        }

        return OK(response, "Cash booking confirmed successfully");
    }

    // 👇 Unchanged — this flow pays for an already-existing booking and has
    // no voucher field on its request DTO, so it keeps using the original
    // 7-argument sendBookingSuccessNotification overload exactly as before.
    @PostMapping("/cash")
    @Operation(summary = "Confirm booking with Counter Cash payment")
    public ResponseEntity<ApiResponse<PaymentResponse>> payWithCash(
            @Parameter(hidden = true) Authentication authentication,
            @Valid @RequestBody PaymentRequest request) {

        User currentUser = getAuthenticatedUser(authentication);

        PaymentResponse paymentResponse = paymentService.processCashPayment(currentUser.getId(), request);

        try {
            telegramService.sendBookingSuccessNotification(
                    paymentResponse.getBookingNumber(),
                    paymentResponse.getMovieTitle(),
                    paymentResponse.getCinemaName(),
                    paymentResponse.getHallName(),
                    paymentResponse.getSeatDetails(),
                    paymentResponse.getConcessionDetails(),
                    paymentResponse.getAmount().doubleValue()
            );
        } catch (Exception e) {
            // Log the error so booking still succeeds even if telegram fails
        }

        return OK(paymentResponse, "Cash booking confirmed successfully");
    }

//    @PostMapping("/khqr/check-md5")
//    @Operation(summary = "Verify Bakong transaction by checking MD5 status")
//    public ResponseEntity<ApiResponse<PaymentResponse>> checkBakongMd5(
//            @RequestBody Map<String, String> payload) {
//
//        String transactionId = payload.get("transactionId");
//        if (transactionId == null || transactionId.isBlank()) {
//            transactionId = payload.get("md5") != null ? payload.get("md5") : "";
//        }
//
//        return OK(paymentService.verifyBakongPayment(transactionId), "Payment verified successfully");
//    }

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

    @PostMapping("/khqr/generate")
    @Operation(summary = "Generate a Bakong KHQR code for an existing booking")
    public ResponseEntity<ApiResponse<PaymentResponse>> generateKhqr(
            @Parameter(hidden = true) Authentication authentication,
            @Valid @RequestBody KhqrGenerateRequest request) {

        User currentUser = getAuthenticatedUser(authentication);
        PaymentResponse response = paymentService.generateKhqr(currentUser.getId(), request);

        return OK(response, "KHQR payment generated successfully");
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