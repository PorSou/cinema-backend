package com.ps.cinema_back.booking.controller;

import com.ps.cinema_back.booking.dto.response.TicketCheckInResponse;
import com.ps.cinema_back.booking.entity.Booking;
import com.ps.cinema_back.booking.repository.BookingRepository;
import com.ps.cinema_back.common.controller.BaseController;
import com.ps.cinema_back.common.enums.BookingStatus;
import com.ps.cinema_back.common.exception.BadRequestException;
import com.ps.cinema_back.common.exception.ConflictException;
import com.ps.cinema_back.common.exception.ResourceNotFoundException;
import com.ps.cinema_back.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/tickets")
@RequiredArgsConstructor
@Tag(name = "Staff Ticket Check-In", description = "Endpoints for cinema staff to scan and validate tickets at the gate")
public class TicketCheckInController extends BaseController {

    private final BookingRepository bookingRepository;

    @PostMapping("/scan/{bookingNumber}")
    @PreAuthorize("hasAnyRole('STAFF', 'ADMIN')")
    @Transactional
    @Operation(summary = "Scan and check in a customer ticket (Staff/Admin only)")
    public ResponseEntity<ApiResponse<TicketCheckInResponse>> scanTicket(
            @PathVariable String bookingNumber,
            @Parameter(hidden = true) Authentication authentication) {

        // Remove "TICKET:" prefix if scanner included it
        String cleanNumber = bookingNumber.replace("TICKET:", "").trim();

        Booking booking = bookingRepository.findByBookingNumberAndIsDeletedFalse(cleanNumber)
                .orElseThrow(() -> new ResourceNotFoundException("No ticket found with booking number: " + cleanNumber));

        // 1. Validate status
        if (booking.getStatus() == BookingStatus.PENDING) {
            throw new BadRequestException("This booking is still unpaid (PENDING). Cannot check in.");
        }
        if (booking.getStatus() == BookingStatus.CANCELLED) {
            throw new BadRequestException("This booking was CANCELLED or refunded. Access denied.");
        }
        if (booking.getStatus() == BookingStatus.CHECKED_IN) {
            throw new ConflictException("Ticket ALREADY USED! Checked in at: " + booking.getCheckedInAt() + " by " + booking.getCheckedInBy());
        }

        // 2. Mark as CHECKED_IN
        booking.setStatus(BookingStatus.CHECKED_IN);
        booking.setCheckedInAt(LocalDateTime.now());
        booking.setCheckedInBy(authentication.getName());
        Booking savedBooking = bookingRepository.save(booking);

        List<String> seatNumbers = savedBooking.getBookingSeats().stream()
                .map(s -> s.getSeat().getSeatRow() + s.getSeat().getSeatNumber())
                .toList();

        TicketCheckInResponse response = TicketCheckInResponse.builder()
                .bookingNumber(savedBooking.getBookingNumber())
                .status(savedBooking.getStatus().name())
                .movieTitle(savedBooking.getShowtime().getMovie().getTitle())
                .cinemaName(savedBooking.getShowtime().getHall().getCinema().getName())
                .hallName(savedBooking.getShowtime().getHall().getName())
                .showtime(savedBooking.getShowtime().getStartTime())
                .seatNumbers(seatNumbers)
                .customerName(savedBooking.getUser().getFullName())
                .customerEmail(savedBooking.getUser().getEmail())
                .checkedInAt(savedBooking.getCheckedInAt())
                .checkedInBy(savedBooking.getCheckedInBy())
                .message("✅ Valid ticket! Customer cleared for entry.")
                .build();

        return OK(response, "Ticket scanned and validated successfully");
    }
}