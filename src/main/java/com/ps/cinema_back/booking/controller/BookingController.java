package com.ps.cinema_back.booking.controller;

import com.ps.cinema_back.booking.dto.request.BookingRequest;
import com.ps.cinema_back.booking.dto.response.BookingResponse;
import com.ps.cinema_back.booking.dto.response.ShowtimeSeatLayoutResponse;
import com.ps.cinema_back.booking.service.BookingService;
import com.ps.cinema_back.common.controller.BaseController;
import com.ps.cinema_back.common.exception.ResourceNotFoundException;
import com.ps.cinema_back.common.exception.UnauthorizedException;
import com.ps.cinema_back.common.response.ApiResponse;
import com.ps.cinema_back.common.response.PageResponse;
import com.ps.cinema_back.user.entity.User;
import com.ps.cinema_back.user.repository.UserRepository;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/bookings")
@RequiredArgsConstructor
public class BookingController extends BaseController {

    private final BookingService bookingService;
    private final UserRepository userRepository;

    @PostMapping
    public ResponseEntity<ApiResponse<BookingResponse>> createBooking(
            @Parameter(hidden = true) Authentication authentication,
            @Valid @RequestBody BookingRequest request) {

        User currentUser = getAuthenticatedUser(authentication);
        return CREATED(bookingService.createBooking(currentUser.getId(), request), "Booking created successfully");
    }

    @GetMapping("/showtime/{showtimeId}/layout")
    public ResponseEntity<ApiResponse<ShowtimeSeatLayoutResponse>> getSeatLayout(
            @PathVariable Long showtimeId) {
        return OK(bookingService.getSeatAvailabilityForShowtime(showtimeId), "Seat availability layout fetched successfully");
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<BookingResponse>> getBookingById(@PathVariable Long id) {
        return OK(bookingService.getBookingById(id), "Booking retrieved successfully");
    }

    @GetMapping("/number/{bookingNumber}")
    public ResponseEntity<ApiResponse<BookingResponse>> getBookingByNumber(@PathVariable String bookingNumber) {
        return OK(bookingService.getBookingByNumber(bookingNumber), "Booking retrieved successfully");
    }

    @GetMapping("/my-bookings")
    public ResponseEntity<ApiResponse<PageResponse<BookingResponse>>> getMyBookings(
            @Parameter(hidden = true) Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction) {

        User currentUser = getAuthenticatedUser(authentication);
        Sort sort = direction.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageRequest = PageRequest.of(page, size, sort);

        return OK(PageResponse.of(bookingService.getMyBookings(currentUser.getId(), pageRequest)), "User bookings fetched successfully");
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<BookingResponse>>> getAllBookings(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction) {

        Sort sort = direction.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageRequest = PageRequest.of(page, size, sort);

        return OK(PageResponse.of(bookingService.getAllBookings(pageRequest)), "All bookings fetched successfully");
    }

    @PutMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse<BookingResponse>> cancelBooking(
            @PathVariable Long id,
            @Parameter(hidden = true) Authentication authentication) {

        User currentUser = getAuthenticatedUser(authentication);
        return OK(bookingService.cancelBooking(id, currentUser.getId()), "Booking cancelled successfully");
    }

    @PutMapping("/{id}/confirm-cash")
    public ResponseEntity<ApiResponse<BookingResponse>> confirmCashBooking(@PathVariable Long id) {
        return OK(bookingService.confirmCashBooking(id), "Cash booking confirmed successfully");
    }

    private User getAuthenticatedUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new UnauthorizedException("User is not authenticated");
        }
        return userRepository.findByEmailAndIsDeletedFalse(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("Authenticated user record not found"));
    }
}
