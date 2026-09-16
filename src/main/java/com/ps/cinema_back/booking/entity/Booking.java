package com.ps.cinema_back.booking.entity;

import com.ps.cinema_back.common.entity.AuditEntity;
import com.ps.cinema_back.common.enums.BookingStatus;
import com.ps.cinema_back.showtime.entity.Showtime;
import com.ps.cinema_back.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "bookings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Booking extends AuditEntity {

    @Column(name = "booking_number", nullable = false, unique = true, length = 64)
    private String bookingNumber; // e.g. BK-20260815-A1B2C3

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private BookingStatus status = BookingStatus.PENDING;

    // 👇 NEW: this is now the FINAL amount charged (subtotal - discountAmount).
    // Previously this held the raw subtotal with no voucher applied, which is
    // why the ticket page and Telegram bot always showed the pre-discount price.
    @Column(name = "total_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalAmount;

    // 👇 NEW: how much was knocked off by the voucher, if any. Defaults to
    // ZERO so existing rows / bookings without a voucher are unaffected.
    @Column(name = "discount_amount", nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal discountAmount = BigDecimal.ZERO;

    // 👇 NEW: which voucher code was redeemed, kept for the receipt/ticket
    // and for support/audit purposes. Null when no voucher was used.
    @Column(name = "voucher_code", length = 64)
    private String voucherCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "showtime_id", nullable = false)
    private Showtime showtime;

    @OneToMany(mappedBy = "booking", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<BookingSeat> bookingSeats = new ArrayList<>();

    @Builder.Default
    @Column(name = "is_deleted", nullable = false)
    private Boolean isDeleted = false;

    @Column(name = "checked_in_at")
    private LocalDateTime checkedInAt;

    @Column(name = "checked_in_by")
    private String checkedInBy; // Staff email
}