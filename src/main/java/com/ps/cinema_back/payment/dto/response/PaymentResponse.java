package com.ps.cinema_back.payment.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.ps.cinema_back.common.enums.BookingStatus;
import com.ps.cinema_back.common.enums.PaymentMethod;
import com.ps.cinema_back.common.enums.PaymentStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PaymentResponse {

    private Long id;
    private String transactionId;
    private PaymentMethod paymentMethod;
    private PaymentStatus paymentStatus;
    private BigDecimal amount;
    private String currency;

    // KHQR Specifics
    private String qrCodeRaw;
    private String qrCodeImageBase64; // Data URI for direct frontend rendering

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime paidAt;

    // Booking Details
    private Long bookingId;
    private String bookingNumber;
    private BookingStatus bookingStatus;

    // 👇 NEW — how much was discounted off this booking's total, and which
    // voucher code did it. Populated from Booking.discountAmount /
    // Booking.voucherCode in PaymentServiceImpl.mapToResponse(), and
    // consumed by PaymentController to build the Telegram notification.
    private BigDecimal discountAmount;
    private String voucherCode;

    // Customer & Cinema details
    private String customerName;
    private String customerEmail;
    private String movieTitle;
    private String cinemaName;
    private String hallName;
    private Integer ticketCount;

    private LocalDateTime createdAt;

    private List<String> seatDetails;
    private List<String> concessionDetails;
}