package com.ps.cinema_back.payment.entity;

import com.ps.cinema_back.booking.entity.Booking;
import com.ps.cinema_back.common.entity.AuditEntity;
import com.ps.cinema_back.common.enums.PaymentMethod;
import com.ps.cinema_back.common.enums.PaymentStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "payments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payment extends AuditEntity {

    @Column(name = "transaction_id", nullable = false, unique = true, length = 64)
    private String transactionId; // e.g., BAKONG-20260815-998822

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false)
    private PaymentMethod paymentMethod;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", nullable = false)
    @Builder.Default
    private PaymentStatus paymentStatus = PaymentStatus.PENDING;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 10)
    @Builder.Default
    private String currency = "USD"; // USD or KHR

    @Column(name = "qr_code_raw", columnDefinition = "TEXT")
    private String qrCodeRaw; // Standard KHQR string format

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id", nullable = false, unique = true)
    private Booking booking;

    @Builder.Default
    @Column(name = "is_deleted", nullable = false)
    private Boolean isDeleted = false;
}