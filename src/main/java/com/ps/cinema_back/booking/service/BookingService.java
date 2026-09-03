package com.ps.cinema_back.booking.service;

import com.ps.cinema_back.booking.dto.request.BookingRequest;
import com.ps.cinema_back.booking.dto.response.BookingResponse;
import com.ps.cinema_back.booking.dto.response.ShowtimeSeatLayoutResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface BookingService {
    BookingResponse createBooking(Long currentUserId, BookingRequest request);
    BookingResponse getBookingById(Long id);
    BookingResponse getBookingByNumber(String bookingNumber);
    Page<BookingResponse> getMyBookings(Long currentUserId, Pageable pageable);
    Page<BookingResponse> getAllBookings(Pageable pageable);
    BookingResponse cancelBooking(Long id, Long currentUserId);
    ShowtimeSeatLayoutResponse getSeatAvailabilityForShowtime(Long showtimeId);
}
