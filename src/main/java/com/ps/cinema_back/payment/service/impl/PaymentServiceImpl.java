package com.ps.cinema_back.payment.service.impl;

import com.ps.cinema_back.auth.service.EmailService;
import com.ps.cinema_back.booking.entity.Booking;
import com.ps.cinema_back.booking.repository.BookingRepository;
import com.ps.cinema_back.common.enums.BookingStatus;
import com.ps.cinema_back.common.enums.PaymentMethod;
import com.ps.cinema_back.common.enums.PaymentStatus;
import com.ps.cinema_back.common.exception.BadRequestException;
import com.ps.cinema_back.common.exception.ConflictException;
import com.ps.cinema_back.common.exception.ResourceNotFoundException;
import com.ps.cinema_back.payment.client.BakongClient;
import com.ps.cinema_back.payment.config.BakongProperties;
import com.ps.cinema_back.payment.dto.request.KhqrGenerateRequest;
import com.ps.cinema_back.payment.dto.response.BakongCheckMd5Response;
import com.ps.cinema_back.payment.dto.response.PaymentResponse;
import com.ps.cinema_back.payment.entity.Payment;
import com.ps.cinema_back.payment.repository.PaymentRepository;
import com.ps.cinema_back.payment.service.PaymentService;
import com.ps.cinema_back.payment.util.BakongKhqrHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final BookingRepository bookingRepository;
    private final BakongClient bakongClient;
    private final BakongProperties bakongProperties;
    private final EmailService emailService;

    @Override
    @Transactional
    public PaymentResponse generateKhqr(Long currentUserId, KhqrGenerateRequest request) {
        Booking booking = bookingRepository.findByIdAndIsDeletedFalse(request.getBookingId())
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with id: " + request.getBookingId()));

        if (booking.getUser() != null && !booking.getUser().getId().equals(currentUserId)) {
            throw new BadRequestException("You can only generate payments for your own booking.");
        }

        if (booking.getStatus() == BookingStatus.CANCELLED) {
            throw new BadRequestException("Cannot pay for a cancelled booking.");
        }
        if (booking.getStatus() == BookingStatus.CONFIRMED || booking.getStatus() == BookingStatus.CHECKED_IN) {
            throw new ConflictException("This booking is already paid and confirmed.");
        }

        // Check if a payment record already exists
        Payment payment = paymentRepository.findByBookingIdAndIsDeletedFalse(booking.getId()).orElse(null);

        String currency = (request.getCurrency() != null && request.getCurrency().equalsIgnoreCase("KHR")) ? "KHR" : "USD";

        // If payment already exists, safely return it instead of throwing a conflict error
        if (payment != null) {
            return mapToResponse(payment);
        }

        String txnId = generateTransactionId();

        // Generate raw KHQR payload with valid CRC16 checksum
        String rawKhqr = BakongKhqrHelper.generateKhqrPayload(
                bakongProperties.getMerchant().getAccount(),
                bakongProperties.getMerchant().getName(),
                bakongProperties.getMerchant().getCity(),
                booking.getTotalAmount(),
                currency,
                txnId
        );

        try {
            payment = Payment.builder()
                    .transactionId(txnId)
                    .paymentMethod(PaymentMethod.KHQR_BAKONG)
                    .paymentStatus(PaymentStatus.PENDING)
                    .amount(booking.getTotalAmount())
                    .currency(currency)
                    .qrCodeRaw(rawKhqr)
                    .booking(booking)
                    .isDeleted(false)
                    .build();

            payment = paymentRepository.save(payment);
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            // Fallback safety catch for concurrent dual-requests: fetch existing record if race condition occurred
            payment = paymentRepository.findByBookingIdAndIsDeletedFalse(booking.getId())
                    .orElseThrow(() -> new ConflictException("Payment generation conflict occurred. Please try again."));
        }

        return mapToResponse(payment);
    }

    @Override
    @Transactional
    public PaymentResponse verifyBakongPayment(String transactionId) {
        Payment payment = paymentRepository.findByTransactionIdAndIsDeletedFalse(transactionId)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found: " + transactionId));

        if (payment.getPaymentStatus() == PaymentStatus.COMPLETED) {
            return mapToResponse(payment);
        }

        String md5Hash = BakongKhqrHelper.calculateKhqrMd5(payment.getQrCodeRaw());
        log.info("Verifying transaction {} with Bakong MD5: {}", transactionId, md5Hash);

        BakongCheckMd5Response bakongResponse = bakongClient.checkTransactionByMd5(md5Hash);

        boolean isLocalTestingMode = true; // Change to false for live production environment

        if (!isLocalTestingMode) {
            if (bakongProperties.getMerchant().getToken() != null && !bakongProperties.getMerchant().getToken().isBlank()) {
                if (bakongResponse == null || !bakongResponse.isSuccessful()) {
                    throw new BadRequestException("Payment is not completed yet or transaction was not found in Bakong network.");
                }

                BigDecimal paidAmount = bakongResponse.getData().getAmount();
                if (paidAmount.compareTo(payment.getAmount()) < 0) {
                    throw new BadRequestException(String.format("Underpaid amount. Expected %s, received %s", payment.getAmount(), paidAmount));
                }
            }
        } else {
            log.warn("⚠️ [DEV MODE] Bypassing strict Bakong live network check for transaction: {}", transactionId);
        }

        payment.setPaymentStatus(PaymentStatus.COMPLETED);
        payment.setPaidAt(LocalDateTime.now());
        Payment updatedPayment = paymentRepository.save(payment);

        Booking booking = payment.getBooking();
        if (booking != null) {
            booking.setStatus(BookingStatus.CONFIRMED);
        }

        try {
            if (booking != null) {
                emailService.sendETicketEmail(booking);
            }
        } catch (Exception e) {
            log.error("⚠️ Failed to send E-Ticket email, but payment was successful: {}", e.getMessage());
        }

        return mapToResponse(updatedPayment);
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentResponse getPaymentById(Long id) {
        Payment payment = paymentRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found with id: " + id));
        return mapToResponse(payment);
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentResponse getPaymentByTransactionId(String transactionId) {
        Payment payment = paymentRepository.findByTransactionIdAndIsDeletedFalse(transactionId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found with transaction: " + transactionId));
        return mapToResponse(payment);
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentResponse getPaymentByBookingId(Long bookingId) {
        Payment payment = paymentRepository.findByBookingIdAndIsDeletedFalse(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found for booking id: " + bookingId));
        return mapToResponse(payment);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PaymentResponse> getAllPayments(Pageable pageable) {
        return paymentRepository.findAllByIsDeletedFalse(pageable).map(this::mapToResponse);
    }

    @Override
    @Transactional
    public PaymentResponse refundPayment(Long id) {
        Payment payment = paymentRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found with id: " + id));

        if (payment.getPaymentStatus() == PaymentStatus.REFUNDED) {
            throw new BadRequestException("Payment is already refunded.");
        }

        payment.setPaymentStatus(PaymentStatus.REFUNDED);
        Payment savedPayment = paymentRepository.save(payment);

        Booking booking = payment.getBooking();
        if (booking != null) {
            booking.setStatus(BookingStatus.CANCELLED);
        }

        return mapToResponse(savedPayment);
    }

    private String generateTransactionId() {
        String datePrefix = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String randomSuffix = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        return "BAKONG-" + datePrefix + "-" + randomSuffix;
    }

    private PaymentResponse mapToResponse(Payment payment) {
        Booking booking = payment.getBooking();

        String qrImageBase64 = null;
        if (payment.getQrCodeRaw() != null) {
            try {
                qrImageBase64 = BakongKhqrHelper.generateQrBase64Image(payment.getQrCodeRaw(), 300, 300);
            } catch (Exception e) {
                log.error("Failed to generate QR base64 image: {}", e.getMessage());
            }
        }

        return PaymentResponse.builder()
                .id(payment.getId())
                .transactionId(payment.getTransactionId())
                .paymentMethod(payment.getPaymentMethod())
                .paymentStatus(payment.getPaymentStatus())
                .amount(payment.getAmount())
                .currency(payment.getCurrency())
                .qrCodeRaw(payment.getQrCodeRaw())
                .qrCodeImageBase64(qrImageBase64)
                .paidAt(payment.getPaidAt())
                .bookingId(booking != null ? booking.getId() : null)
                .bookingNumber(booking != null ? booking.getBookingNumber() : null)
                .bookingStatus(booking != null ? booking.getStatus() : null)
                .customerName(booking != null && booking.getUser() != null ? booking.getUser().getFullName() : null)
                .customerEmail(booking != null && booking.getUser() != null ? booking.getUser().getEmail() : null)
                .movieTitle(booking != null && booking.getShowtime() != null && booking.getShowtime().getMovie() != null ? booking.getShowtime().getMovie().getTitle() : null)
                .cinemaName(booking != null && booking.getShowtime() != null && booking.getShowtime().getHall() != null && booking.getShowtime().getHall().getCinema() != null ? booking.getShowtime().getHall().getCinema().getName() : null)
                .hallName(booking != null && booking.getShowtime() != null && booking.getShowtime().getHall() != null ? booking.getShowtime().getHall().getName() : null)
                .ticketCount(booking != null && booking.getBookingSeats() != null ? booking.getBookingSeats().size() : 0)
                .createdAt(payment.getCreatedAt())
                .build();
    }
}