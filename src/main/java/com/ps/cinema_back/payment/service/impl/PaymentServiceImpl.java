package com.ps.cinema_back.payment.service.impl;

import com.ps.cinema_back.auth.service.EmailService;
import com.ps.cinema_back.booking.dto.request.BookingRequest;
import com.ps.cinema_back.booking.dto.response.BookingResponse;
import com.ps.cinema_back.booking.entity.Booking;
import com.ps.cinema_back.booking.repository.BookingRepository;
import com.ps.cinema_back.booking.service.BookingService;
import com.ps.cinema_back.common.enums.BookingStatus;
import com.ps.cinema_back.common.enums.PaymentMethod;
import com.ps.cinema_back.common.enums.PaymentStatus;
import com.ps.cinema_back.common.exception.BadRequestException;
import com.ps.cinema_back.common.exception.ConflictException;
import com.ps.cinema_back.common.exception.ResourceNotFoundException;
import com.ps.cinema_back.concession.entity.BookingConcessionItem;
import com.ps.cinema_back.concession.repository.BookingConcessionItemRepository;
import com.ps.cinema_back.notification.service.NotificationService;
import com.ps.cinema_back.payment.client.BakongClient;
import com.ps.cinema_back.payment.config.BakongProperties;
import com.ps.cinema_back.payment.dto.request.CashBookingRequest;
import com.ps.cinema_back.payment.dto.request.KhqrGenerateRequest;
import com.ps.cinema_back.payment.dto.request.PaymentRequest;
import com.ps.cinema_back.payment.dto.response.BakongCheckMd5Response;
import com.ps.cinema_back.payment.dto.response.PaymentResponse;
import com.ps.cinema_back.payment.entity.Payment;
import com.ps.cinema_back.payment.repository.PaymentRepository;
import com.ps.cinema_back.payment.service.PaymentService;
import com.ps.cinema_back.payment.util.BakongKhqrHelper;
import com.ps.cinema_back.telegrambot.service.TelegramService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final BookingRepository bookingRepository;
    private final BakongClient bakongClient;
    private final BakongProperties bakongProperties;
    private final EmailService emailService;
    private final BookingService bookingService;
    private final BookingConcessionItemRepository bookingConcessionItemRepository;
    private final NotificationService notificationService;
    private final TelegramService telegramService;

    @Value("${bakong.testing-mode:false}")
    private boolean isLocalTestingMode;


    // =========================================================
    // CHECK PAYMENT STATUS BY TRANSACTION
    // =========================================================

    @Transactional
    public BakongCheckMd5Response checkPaymentStatus(String transactionId) {

        Payment payment = paymentRepository
                .findByTransactionIdAndIsDeletedFalse(transactionId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Transaction not found: " + transactionId
                        )
                );

        if (payment.getPaymentStatus() == PaymentStatus.COMPLETED) {

            BakongCheckMd5Response already =
                    new BakongCheckMd5Response();

            already.setResponseCode(0);
            already.setResponseMessage("Already completed");

            return already;
        }

        if (payment.getQrCodeRaw() == null ||
                payment.getQrCodeRaw().isBlank()) {

            throw new BadRequestException(
                    "KHQR data is missing for this payment."
            );
        }

        String md5Hash =
                BakongKhqrHelper.calculateKhqrMd5(
                        payment.getQrCodeRaw()
                );

        log.info(
                "Checking Bakong payment transaction={} md5={}",
                transactionId,
                md5Hash
        );

        BakongCheckMd5Response bakongResponse =
                bakongClient.checkTransactionByMd5(md5Hash);

        log.info(
                "Bakong check-md5 response for transaction={}: successful={}, raw={}",
                transactionId,
                bakongResponse != null && bakongResponse.isSuccessful(),
                bakongResponse
        );

        if (bakongResponse != null &&
                bakongResponse.isSuccessful()) {

            payment.setPaymentStatus(PaymentStatus.COMPLETED);
            payment.setPaidAt(LocalDateTime.now());

            Payment savedPayment = paymentRepository.save(payment);

            Booking booking = payment.getBooking();

            if (booking != null) {

                booking.setStatus(BookingStatus.CONFIRMED);

                bookingRepository.save(booking);
            }

            // Notify Telegram exactly once, at the moment this KHQR payment
            // actually transitions to COMPLETED. Safe against duplicate
            // sends because this whole method returns early above (see the
            // PaymentStatus.COMPLETED check at the top) once a payment is
            // already completed, so repeated polling from the frontend
            // after this point never re-enters here.
            try {
                PaymentResponse resp = mapToResponse(savedPayment);
                telegramService.sendKhqrPaymentSuccessNotification(
                        resp.getBookingNumber(),
                        resp.getMovieTitle(),
                        resp.getCinemaName(),
                        resp.getHallName(),
                        resp.getSeatDetails(),
                        resp.getConcessionDetails(),
                        resp.getAmount() != null ? resp.getAmount().doubleValue() : null,
                        resp.getDiscountAmount() != null ? resp.getDiscountAmount().doubleValue() : null,
                        resp.getVoucherCode()
                );
            } catch (Exception e) {
                // Booking/payment still succeeds even if Telegram fails,
                // but log it so failures are visible instead of silent.
                log.error(
                        "Failed to send KHQR Telegram notification (checkPaymentStatus) for transaction={}: {}",
                        transactionId,
                        e.getMessage(),
                        e
                );
            }
        }

        return bakongResponse != null
                ? bakongResponse
                : new BakongCheckMd5Response();
    }


    // =========================================================
    // CASH BOOKING + PAYMENT
    // =========================================================

    @Override
    @Transactional
    public PaymentResponse bookAndPayCash(
            Long userId,
            CashBookingRequest request
    ) {

        BookingRequest bookingRequest =
                new BookingRequest();

        bookingRequest.setShowtimeId(
                request.getShowtimeId()
        );

        bookingRequest.setSeatIds(
                request.getSeatIds()
        );

        bookingRequest.setConcessions(
                request.getConcessions()
        );

        BookingResponse booking =
                bookingService.createBooking(
                        userId,
                        bookingRequest,
                        request.getVoucherCode()
                );

        PaymentRequest paymentRequest =
                new PaymentRequest();

        paymentRequest.setBookingId(
                booking.getId()
        );

        return processCashPayment(
                userId,
                paymentRequest
        );
    }


    // =========================================================
    // PROCESS CASH PAYMENT
    // =========================================================

    @Override
    @Transactional
    public PaymentResponse processCashPayment(
            Long currentUserId,
            PaymentRequest request
    ) {

        Booking booking =
                bookingRepository
                        .findByIdAndIsDeletedFalse(
                                request.getBookingId()
                        )
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Booking not found with id: "
                                                + request.getBookingId()
                                )
                        );

        if (booking.getUser() != null &&
                !booking.getUser().getId()
                        .equals(currentUserId)) {

            throw new BadRequestException(
                    "You can only pay for your own booking."
            );
        }

        if (booking.getStatus() ==
                BookingStatus.CANCELLED) {

            throw new BadRequestException(
                    "Cannot pay for a cancelled booking."
            );
        }

        if (booking.getStatus() ==
                BookingStatus.CONFIRMED ||
                booking.getStatus() ==
                        BookingStatus.CHECKED_IN) {

            throw new ConflictException(
                    "This booking is already paid and confirmed."
            );
        }

        Set<Long> seatIds =
                booking.getBookingSeats()
                        .stream()
                        .map(bs ->
                                bs.getSeat().getId()
                        )
                        .collect(Collectors.toSet());

        List<Long> alreadyTaken =
                bookingRepository.findAlreadyBookedSeatIds(
                        booking.getShowtime().getId(),
                        List.of(
                                BookingStatus.CONFIRMED,
                                BookingStatus.CHECKED_IN
                        ),
                        seatIds
                );

        if (!alreadyTaken.isEmpty()) {

            booking.setStatus(
                    BookingStatus.CANCELLED
            );

            bookingRepository.save(booking);

            throw new ConflictException(
                    "Sorry, one or more of your selected seats were just booked by another customer. Please choose again."
            );
        }

        Payment payment =
                paymentRepository
                        .findByBookingIdAndIsDeletedFalse(
                                booking.getId()
                        )
                        .orElse(null);

        if (payment == null) {

            String txnId =
                    "CASH-"
                            + LocalDateTime.now()
                            .format(
                                    DateTimeFormatter.ofPattern(
                                            "yyyyMMdd"
                                    )
                            )
                            + "-"
                            + UUID.randomUUID()
                            .toString()
                            .substring(0, 8)
                            .toUpperCase();

            payment = Payment.builder()
                    .transactionId(txnId)
                    .paymentMethod(
                            PaymentMethod.CASH
                    )
                    .paymentStatus(
                            PaymentStatus.COMPLETED
                    )
                    .amount(
                            booking.getTotalAmount()
                    )
                    .currency("USD")
                    .paidAt(
                            LocalDateTime.now()
                    )
                    .booking(booking)
                    .isDeleted(false)
                    .build();

        } else {

            payment.setPaymentMethod(
                    PaymentMethod.CASH
            );

            payment.setPaymentStatus(
                    PaymentStatus.COMPLETED
            );

            payment.setPaidAt(
                    LocalDateTime.now()
            );
        }

        payment =
                paymentRepository.save(payment);

        booking.setStatus(
                BookingStatus.CONFIRMED
        );

        bookingRepository.save(booking);

        try {

            notificationService.createNotification(
                    "Cash Payment Received",
                    "Cash payment of $"
                            + payment.getAmount()
                            + " verified for booking #"
                            + booking.getBookingNumber(),
                    "PAYMENT"
            );

        } catch (Exception e) {
            log.warn(
                    "Failed to create in-app notification for cash payment on booking {}: {}",
                    booking.getBookingNumber(),
                    e.getMessage()
            );
        }

        return mapToResponse(payment);
    }


    // =========================================================
    // GENERATE KHQR
    // =========================================================

    @Override
    @Transactional
    public PaymentResponse generateKhqr(
            Long currentUserId,
            KhqrGenerateRequest request
    ) {

        Booking booking =
                bookingRepository
                        .findByIdAndIsDeletedFalse(
                                request.getBookingId()
                        )
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Booking not found with id: "
                                                + request.getBookingId()
                                )
                        );

        if (booking.getUser() != null &&
                !booking.getUser().getId()
                        .equals(currentUserId)) {

            throw new BadRequestException(
                    "You can only generate payments for your own booking."
            );
        }

        if (booking.getStatus() ==
                BookingStatus.CANCELLED) {

            throw new BadRequestException(
                    "Cannot pay for a cancelled booking."
            );
        }

        if (booking.getStatus() ==
                BookingStatus.CONFIRMED ||
                booking.getStatus() ==
                        BookingStatus.CHECKED_IN) {

            throw new ConflictException(
                    "This booking is already paid and confirmed."
            );
        }

        Payment payment =
                paymentRepository
                        .findByBookingIdAndIsDeletedFalse(
                                booking.getId()
                        )
                        .orElse(null);

        String currency =
                request.getCurrency() != null &&
                        request.getCurrency()
                                .equalsIgnoreCase("KHR")
                        ? "KHR"
                        : "USD";

        if (payment != null) {
            return mapToResponse(payment);
        }

        String txnId =
                generateTransactionId();

        String rawKhqr =
                BakongKhqrHelper.generateKhqrPayload(
                        bakongProperties
                                .getAccount()
                                .getId(),

                        bakongProperties
                                .getMerchant()
                                .getName(),

                        bakongProperties
                                .getMerchant()
                                .getCity(),

                        booking.getTotalAmount(),

                        currency,

                        txnId
                );

        try {

            payment = Payment.builder()
                    .transactionId(txnId)
                    .paymentMethod(
                            PaymentMethod.KHQR_BAKONG
                    )
                    .paymentStatus(
                            PaymentStatus.PENDING
                    )
                    .amount(
                            booking.getTotalAmount()
                    )
                    .currency(currency)
                    .qrCodeRaw(rawKhqr)
                    .booking(booking)
                    .isDeleted(false)
                    .build();

            payment =
                    paymentRepository.save(payment);

        } catch (DataIntegrityViolationException e) {

            payment =
                    paymentRepository
                            .findByBookingIdAndIsDeletedFalse(
                                    booking.getId()
                            )
                            .orElseThrow(() ->
                                    new ConflictException(
                                            "Payment generation conflict occurred. Please try again."
                                    )
                            );
        }

        return mapToResponse(payment);
    }


    // =========================================================
    // VERIFY BAKONG PAYMENT
    // =========================================================

    @Override
    @Transactional
    public PaymentResponse verifyBakongPayment(
            String transactionId
    ) {

        Payment payment =
                paymentRepository
                        .findByTransactionIdAndIsDeletedFalse(
                                transactionId
                        )
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Transaction not found: "
                                                + transactionId
                                )
                        );

        if (payment.getPaymentStatus() ==
                PaymentStatus.COMPLETED) {

            return mapToResponse(payment);
        }

        if (payment.getQrCodeRaw() == null ||
                payment.getQrCodeRaw().isBlank()) {

            throw new BadRequestException(
                    "KHQR data is missing for this payment."
            );
        }

        String md5Hash =
                BakongKhqrHelper.calculateKhqrMd5(
                        payment.getQrCodeRaw()
                );

        log.info(
                "Verifying transaction {} with Bakong MD5: {}",
                transactionId,
                md5Hash
        );

        BakongCheckMd5Response bakongResponse =
                bakongClient.checkTransactionByMd5(
                        md5Hash
                );

        // =====================================================
        // LIVE MODE
        // =====================================================

        if (!isLocalTestingMode) {

            if (bakongResponse == null ||
                    !bakongResponse.isSuccessful()) {

                throw new BadRequestException(
                        "Payment is not completed yet or transaction was not found in Bakong network."
                );
            }

            if (bakongResponse.getData() == null) {

                throw new BadRequestException(
                        "Bakong returned an invalid payment response."
                );
            }

            BigDecimal paidAmount =
                    bakongResponse
                            .getData()
                            .getAmount();

            if (paidAmount == null) {

                throw new BadRequestException(
                        "Bakong payment amount is missing."
                );
            }

            if (paidAmount.compareTo(
                    payment.getAmount()
            ) < 0) {

                throw new BadRequestException(
                        String.format(
                                "Underpaid amount. Expected %s, received %s",
                                payment.getAmount(),
                                paidAmount
                        )
                );
            }
        }

        // =====================================================
        // LOCAL TESTING MODE
        // =====================================================

        else {

            log.warn(
                    "⚠️ [DEV MODE] Bypassing strict Bakong live network check for transaction: {}",
                    transactionId
            );
        }

        // =====================================================
        // MARK PAYMENT COMPLETED
        // =====================================================

        payment.setPaymentStatus(
                PaymentStatus.COMPLETED
        );

        payment.setPaidAt(
                LocalDateTime.now()
        );

        Payment updatedPayment =
                paymentRepository.save(payment);

        Booking booking =
                payment.getBooking();

        if (booking != null) {

            booking.setStatus(
                    BookingStatus.CONFIRMED
            );

            bookingRepository.save(booking);
        }

        // =====================================================
        // TELEGRAM NOTIFICATION
        // =====================================================
        // NEW — this endpoint (/payments/verify-bakong/{transactionId}) was
        // completing KHQR payments and confirming bookings WITHOUT ever
        // notifying Telegram. If anything (a webhook, an admin action, a
        // manual call) hits this route before the customer's own
        // check-md5 polling loop does, the payment flips to COMPLETED here
        // silently, and the polling loop then just sees "already
        // completed" and returns early in checkPaymentStatus() without
        // ever reaching ITS Telegram call either. This block closes that
        // gap so either completion path notifies exactly once.

        try {

            if (booking != null) {

                PaymentResponse resp = mapToResponse(updatedPayment);

                telegramService.sendKhqrPaymentSuccessNotification(
                        resp.getBookingNumber(),
                        resp.getMovieTitle(),
                        resp.getCinemaName(),
                        resp.getHallName(),
                        resp.getSeatDetails(),
                        resp.getConcessionDetails(),
                        resp.getAmount() != null ? resp.getAmount().doubleValue() : null,
                        resp.getDiscountAmount() != null ? resp.getDiscountAmount().doubleValue() : null,
                        resp.getVoucherCode()
                );
            }

        } catch (Exception e) {

            log.error(
                    "Failed to send KHQR Telegram notification (verifyBakongPayment) for transaction={}: {}",
                    transactionId,
                    e.getMessage(),
                    e
            );
        }

        // =====================================================
        // NOTIFICATION
        // =====================================================

        try {

            if (booking != null) {

                notificationService.createNotification(
                        "KHQR Payment Completed",
                        "Bakong payment of $"
                                + payment.getAmount()
                                + " successfully verified for booking #"
                                + booking.getBookingNumber(),
                        "PAYMENT"
                );
            }

        } catch (Exception e) {
            log.warn(
                    "Failed to create in-app notification for KHQR payment on transaction {}: {}",
                    transactionId,
                    e.getMessage()
            );
        }

        // =====================================================
        // E-TICKET EMAIL
        // =====================================================

        try {

            if (booking != null) {

                emailService.sendETicketEmail(
                        booking
                );
            }

        } catch (Exception e) {

            log.error(
                    "⚠️ Failed to send E-Ticket email, but payment was successful: {}",
                    e.getMessage()
            );
        }

        return mapToResponse(updatedPayment);
    }


    // =========================================================
    // GET PAYMENT BY ID
    // =========================================================

    @Override
    @Transactional(readOnly = true)
    public PaymentResponse getPaymentById(Long id) {

        Payment payment =
                paymentRepository
                        .findByIdAndIsDeletedFalse(id)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Payment not found with id: "
                                                + id
                                )
                        );

        return mapToResponse(payment);
    }


    // =========================================================
    // GET PAYMENT BY TRANSACTION ID
    // =========================================================

    @Override
    @Transactional(readOnly = true)
    public PaymentResponse getPaymentByTransactionId(
            String transactionId
    ) {

        Payment payment =
                paymentRepository
                        .findByTransactionIdAndIsDeletedFalse(
                                transactionId
                        )
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Payment not found with transaction: "
                                                + transactionId
                                )
                        );

        return mapToResponse(payment);
    }


    // =========================================================
    // GET PAYMENT BY BOOKING ID
    // =========================================================

    @Override
    @Transactional(readOnly = true)
    public PaymentResponse getPaymentByBookingId(
            Long bookingId
    ) {

        Payment payment =
                paymentRepository
                        .findByBookingIdAndIsDeletedFalse(
                                bookingId
                        )
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Payment not found for booking id: "
                                                + bookingId
                                )
                        );

        return mapToResponse(payment);
    }


    // =========================================================
    // GET ALL PAYMENTS
    // =========================================================

    @Override
    @Transactional(readOnly = true)
    public Page<PaymentResponse> getAllPayments(
            Pageable pageable
    ) {

        return paymentRepository
                .findAllByIsDeletedFalse(pageable)
                .map(this::mapToResponse);
    }


    // =========================================================
    // REFUND
    // =========================================================

    @Override
    @Transactional
    public PaymentResponse refundPayment(Long id) {

        Payment payment =
                paymentRepository
                        .findByIdAndIsDeletedFalse(id)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Payment not found with id: "
                                                + id
                                )
                        );

        if (payment.getPaymentStatus() ==
                PaymentStatus.REFUNDED) {

            throw new BadRequestException(
                    "Payment is already refunded."
            );
        }

        payment.setPaymentStatus(
                PaymentStatus.REFUNDED
        );

        Payment savedPayment =
                paymentRepository.save(payment);

        Booking booking =
                payment.getBooking();

        if (booking != null) {

            booking.setStatus(
                    BookingStatus.CANCELLED
            );

            bookingRepository.save(booking);
        }

        try {

            if (booking != null) {

                notificationService.createNotification(
                        "Payment Refunded",
                        "Payment for booking #"
                                + booking.getBookingNumber()
                                + " has been refunded.",
                        "SYSTEM"
                );
            }

        } catch (Exception e) {
            log.warn(
                    "Failed to create in-app notification for refund on payment {}: {}",
                    id,
                    e.getMessage()
            );
        }

        return mapToResponse(savedPayment);
    }


    // =========================================================
    // GENERATE TRANSACTION ID
    // =========================================================

    private String generateTransactionId() {

        String datePrefix =
                LocalDateTime.now()
                        .format(
                                DateTimeFormatter.ofPattern(
                                        "yyyyMMdd"
                                )
                        );

        String randomSuffix =
                UUID.randomUUID()
                        .toString()
                        .substring(0, 8)
                        .toUpperCase();

        return "BAKONG-"
                + datePrefix
                + "-"
                + randomSuffix;
    }


    // =========================================================
    // MAP PAYMENT RESPONSE
    // =========================================================

    private PaymentResponse mapToResponse(
            Payment payment
    ) {

        Booking booking =
                payment.getBooking();

        String qrImageBase64 = null;

        if (payment.getQrCodeRaw() != null) {

            try {

                qrImageBase64 =
                        BakongKhqrHelper
                                .generateQrBase64Image(
                                        payment.getQrCodeRaw(),
                                        300,
                                        300
                                );

            } catch (Exception e) {

                log.error(
                        "Failed to generate QR base64 image: {}",
                        e.getMessage()
                );
            }
        }

        List<String> seatDetails =
                (
                        booking != null &&
                                booking.getBookingSeats() != null
                )
                        ? booking.getBookingSeats()
                        .stream()
                        .map(bs ->
                                bs.getSeat().getSeatRow()
                                        + ""
                                        + bs.getSeat().getSeatNumber()
                                        + " ("
                                        + bs.getSeat().getSeatType()
                                        + ")"
                        )
                        .collect(Collectors.toList())
                        : List.of();

        List<String> concessionDetails =
                List.of();

        if (booking != null) {

            List<BookingConcessionItem> items =
                    bookingConcessionItemRepository
                            .findByBookingId(
                                    booking.getId()
                            );

            concessionDetails =
                    items.stream()
                            .map(bc ->
                                    bc.getQuantity()
                                            + "x "
                                            + bc.getConcessionItem()
                                            .getName()
                            )
                            .collect(Collectors.toList());
        }

        return PaymentResponse.builder()
                .id(payment.getId())
                .transactionId(
                        payment.getTransactionId()
                )
                .paymentMethod(
                        payment.getPaymentMethod()
                )
                .paymentStatus(
                        payment.getPaymentStatus()
                )
                .amount(payment.getAmount())
                .currency(payment.getCurrency())
                .qrCodeRaw(payment.getQrCodeRaw())
                .qrCodeImageBase64(qrImageBase64)
                .paidAt(payment.getPaidAt())
                .bookingId(
                        booking != null
                                ? booking.getId()
                                : null
                )
                .bookingNumber(
                        booking != null
                                ? booking.getBookingNumber()
                                : null
                )
                .bookingStatus(
                        booking != null
                                ? booking.getStatus()
                                : null
                )
                .discountAmount(
                        booking != null
                                ? booking.getDiscountAmount()
                                : BigDecimal.ZERO
                )
                .voucherCode(
                        booking != null
                                ? booking.getVoucherCode()
                                : null
                )
                .customerName(
                        booking != null &&
                                booking.getUser() != null
                                ? booking.getUser().getFullName()
                                : null
                )
                .customerEmail(
                        booking != null &&
                                booking.getUser() != null
                                ? booking.getUser().getEmail()
                                : null
                )
                .movieTitle(
                        booking != null &&
                                booking.getShowtime() != null &&
                                booking.getShowtime().getMovie() != null
                                ? booking.getShowtime()
                                .getMovie()
                                .getTitle()
                                : null
                )
                .cinemaName(
                        booking != null &&
                                booking.getShowtime() != null &&
                                booking.getShowtime().getHall() != null &&
                                booking.getShowtime()
                                        .getHall()
                                        .getCinema() != null
                                ? booking.getShowtime()
                                .getHall()
                                .getCinema()
                                .getName()
                                : null
                )
                .hallName(
                        booking != null &&
                                booking.getShowtime() != null &&
                                booking.getShowtime().getHall() != null
                                ? booking.getShowtime()
                                .getHall()
                                .getName()
                                : null
                )
                .ticketCount(
                        booking != null &&
                                booking.getBookingSeats() != null
                                ? booking.getBookingSeats()
                                .size()
                                : 0
                )
                .seatDetails(seatDetails)
                .concessionDetails(
                        concessionDetails
                )
                .createdAt(
                        payment.getCreatedAt()
                )
                .build();
    }
}