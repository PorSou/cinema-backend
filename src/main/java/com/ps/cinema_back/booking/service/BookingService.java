package com.ps.cinema_back.booking.service;

import com.ps.cinema_back.booking.dto.request.BookingRequest;
import com.ps.cinema_back.booking.dto.response.BookingResponse;
import com.ps.cinema_back.booking.dto.response.ShowtimeSeatLayoutResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface BookingService {

    // Unchanged signature — every existing caller (BookingController, the
    // KHQR flow, etc.) keeps working exactly as before with no voucher applied.
    BookingResponse createBooking(Long currentUserId, BookingRequest request);

    // 👇 NEW overload — only the cash-booking flow calls this one, passing
    // the voucher code the customer applied at checkout. The 2-arg version
    // above simply delegates to this with voucherCode = null.
    BookingResponse createBooking(Long currentUserId, BookingRequest request, String voucherCode);

    ShowtimeSeatLayoutResponse getSeatAvailabilityForShowtime(Long showtimeId);

    BookingResponse getBookingById(Long id);

    BookingResponse getBookingByNumber(String bookingNumber);

    Page<BookingResponse> getMyBookings(Long currentUserId, Pageable pageable);

    Page<BookingResponse> getAllBookings(Pageable pageable);

    BookingResponse cancelBooking(Long id, Long currentUserId);

    BookingResponse confirmCashBooking(Long bookingId);
}