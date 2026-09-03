package com.ps.cinema_back.booking.scheduler;

import com.ps.cinema_back.booking.entity.Booking;
import com.ps.cinema_back.booking.repository.BookingRepository;
import com.ps.cinema_back.common.enums.BookingStatus;
import com.ps.cinema_back.common.enums.PaymentStatus;
import com.ps.cinema_back.payment.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class BookingCleanupScheduler {

    private final BookingRepository bookingRepository;
    private final PaymentRepository paymentRepository;

    // Runs every 60,000 milliseconds (1 minute)
    @Scheduled(fixedRate = 60000)
    @Transactional
    public void releaseExpiredPendingBookings() {
        // Expiry threshold: 10 minutes ago
        LocalDateTime expiryThreshold = LocalDateTime.now().minusMinutes(10);

        List<Booking> expiredBookings = bookingRepository
                .findAllByStatusAndCreatedAtBeforeAndIsDeletedFalse(BookingStatus.PENDING, expiryThreshold);

        if (expiredBookings.isEmpty()) {
            return;
        }

        log.info("⏳ Found {} expired pending booking(s). Releasing seats...", expiredBookings.size());

        for (Booking booking : expiredBookings) {
            // Cancel booking to release seats
            booking.setStatus(BookingStatus.CANCELLED);
            bookingRepository.save(booking);

            // Mark associated payment as FAILED
            paymentRepository.findByBookingIdAndIsDeletedFalse(booking.getId()).ifPresent(payment -> {
                if (payment.getPaymentStatus() == PaymentStatus.PENDING) {
                    payment.setPaymentStatus(PaymentStatus.FAILED);
                    paymentRepository.save(payment);
                }
            });

            log.info("❌ Booking #{} expired & cancelled. Seats released.", booking.getBookingNumber());
        }
    }
}