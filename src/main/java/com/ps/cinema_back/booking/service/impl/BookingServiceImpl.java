package com.ps.cinema_back.booking.service.impl;

import com.ps.cinema_back.booking.dto.request.BookingRequest;
import com.ps.cinema_back.booking.dto.response.BookingResponse;
import com.ps.cinema_back.booking.dto.response.ShowtimeSeatLayoutResponse;
import com.ps.cinema_back.booking.dto.response.TicketResponse;
import com.ps.cinema_back.booking.entity.Booking;
import com.ps.cinema_back.booking.entity.BookingSeat;
import com.ps.cinema_back.booking.repository.BookingRepository;
import com.ps.cinema_back.booking.service.BookingService;
import com.ps.cinema_back.common.enums.BookingStatus;
import com.ps.cinema_back.common.enums.SeatType;
import com.ps.cinema_back.common.exception.BadRequestException;
import com.ps.cinema_back.common.exception.ConflictException;
import com.ps.cinema_back.common.exception.ResourceNotFoundException;
import com.ps.cinema_back.seat.entity.Seat;
import com.ps.cinema_back.seat.repository.SeatRepository;
import com.ps.cinema_back.showtime.entity.Showtime;
import com.ps.cinema_back.showtime.repository.ShowtimeRepository;
import com.ps.cinema_back.user.entity.User;
import com.ps.cinema_back.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BookingServiceImpl implements BookingService {

    private final BookingRepository bookingRepository;
    private final ShowtimeRepository showtimeRepository;
    private final SeatRepository seatRepository;
    private final UserRepository userRepository;

    private static final List<BookingStatus> ACTIVE_STATUSES = List.of(BookingStatus.PENDING, BookingStatus.CONFIRMED);

    // Couple/Twin seats are sold as a single flat-price pair ($10.00 for the pair, $5.00 per record)
    private static final BigDecimal COUPLE_PAIR_PRICE = new BigDecimal("10.00");
    private static final BigDecimal COUPLE_SEAT_PRICE =
            COUPLE_PAIR_PRICE.divide(BigDecimal.valueOf(2), 2, RoundingMode.HALF_UP);

    // VIP seats are a flat $10.00 each
    private static final BigDecimal VIP_SEAT_PRICE = new BigDecimal("10.00");

    @Override
    @Transactional
    public BookingResponse createBooking(Long currentUserId, BookingRequest request) {
        User user = userRepository.findByIdAndIsDeletedFalse(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + currentUserId));

        Showtime showtime = showtimeRepository.findByIdAndIsDeletedFalse(request.getShowtimeId())
                .orElseThrow(() -> new ResourceNotFoundException("Showtime not found with id: " + request.getShowtimeId()));

        // Fetch selected seats
        List<Seat> seats = seatRepository.findAllById(request.getSeatIds()).stream()
                .filter(s -> !Boolean.TRUE.equals(s.getIsDeleted()))
                .collect(Collectors.toList());

        if (seats.size() != request.getSeatIds().size()) {
            throw new ResourceNotFoundException("One or more selected seats do not exist or are invalid.");
        }

        // Ensure all seats belong to the showtime's hall
        Long hallId = showtime.getHall().getId();
        for (Seat seat : seats) {
            if (!seat.getHall().getId().equals(hallId)) {
                throw new BadRequestException("Seat " + seat.getSeatRow() + seat.getSeatNumber() + " does not belong to Hall: " + showtime.getHall().getName());
            }
        }

        // Double-Booking Check
        List<Long> alreadyBookedIds = bookingRepository.findAlreadyBookedSeatIds(showtime.getId(), ACTIVE_STATUSES, request.getSeatIds());
        if (!alreadyBookedIds.isEmpty()) {
            throw new ConflictException("One or more selected seats are already booked by another customer.");
        }

        // Calculate pricing per seat and total amount
        BigDecimal totalAmount = BigDecimal.ZERO;
        List<BookingSeat> bookingSeats = new ArrayList<>();

        Booking booking = Booking.builder()
                .bookingNumber(generateBookingNumber())
                .status(BookingStatus.PENDING)
                .user(user)
                .showtime(showtime)
                .totalAmount(BigDecimal.ZERO)
                .isDeleted(false)
                .build();

        for (Seat seat : seats) {
            BigDecimal seatPrice = calculateSeatPrice(showtime.getBasePrice(), seat.getSeatType());
            totalAmount = totalAmount.add(seatPrice);

            BookingSeat bookingSeat = BookingSeat.builder()
                    .booking(booking)
                    .seat(seat)
                    .price(seatPrice)
                    .build();

            bookingSeats.add(bookingSeat);
        }

        booking.setTotalAmount(totalAmount);
        booking.setBookingSeats(bookingSeats);

        Booking savedBooking = bookingRepository.save(booking);
        return mapToResponse(savedBooking);
    }

    @Override
    @Transactional(readOnly = true)
    public ShowtimeSeatLayoutResponse getSeatAvailabilityForShowtime(Long showtimeId) {
        Showtime showtime = showtimeRepository.findByIdAndIsDeletedFalse(showtimeId)
                .orElseThrow(() -> new ResourceNotFoundException("Showtime not found with id: " + showtimeId));

        List<Seat> allSeats = seatRepository.findAllByHallIdAndIsDeletedFalseOrderBySeatRowAscSeatNumberAsc(showtime.getHall().getId());
        Set<Long> bookedSeatIds = bookingRepository.findAllBookedSeatIdsForShowtime(showtimeId, ACTIVE_STATUSES);

        List<ShowtimeSeatLayoutResponse.SeatAvailabilityResponse> seatResponses = allSeats.stream()
                .map(seat -> ShowtimeSeatLayoutResponse.SeatAvailabilityResponse.builder()
                        .seatId(seat.getId())
                        .seatCode(seat.getSeatRow() + seat.getSeatNumber())
                        .seatRow(seat.getSeatRow())
                        .seatNumber(seat.getSeatNumber())
                        .seatType(seat.getSeatType())
                        .gridX(seat.getGridX())
                        .gridY(seat.getGridY())
                        .calculatedPrice(calculateSeatPrice(showtime.getBasePrice(), seat.getSeatType()))
                        .isAvailable(!bookedSeatIds.contains(seat.getId()))
                        .build())
                .collect(Collectors.toList());

        return ShowtimeSeatLayoutResponse.builder()
                .showtimeId(showtime.getId())
                .hallId(showtime.getHall().getId())
                .hallName(showtime.getHall().getName())
                .totalSeats(allSeats.size())
                .seats(seatResponses)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public BookingResponse getBookingById(Long id) {
        Booking booking = bookingRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with id: " + id));
        return mapToResponse(booking);
    }

    @Override
    @Transactional(readOnly = true)
    public BookingResponse getBookingByNumber(String bookingNumber) {
        Booking booking = bookingRepository.findByBookingNumberAndIsDeletedFalse(bookingNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with number: " + bookingNumber));
        return mapToResponse(booking);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<BookingResponse> getMyBookings(Long currentUserId, Pageable pageable) {
        return bookingRepository.findAllByUserIdAndIsDeletedFalse(currentUserId, pageable)
                .map(this::mapToResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<BookingResponse> getAllBookings(Pageable pageable) {
        return bookingRepository.findAllByIsDeletedFalse(pageable)
                .map(this::mapToResponse);
    }

    @Override
    @Transactional
    public BookingResponse cancelBooking(Long id, Long currentUserId) {
        Booking booking = bookingRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with id: " + id));

        if (booking.getStatus() == BookingStatus.CANCELLED) {
            throw new BadRequestException("Booking is already cancelled.");
        }

        // Optional security validation: ensure the user owns the booking or is authorized
        if (currentUserId != null && !booking.getUser().getId().equals(currentUserId)) {
            // If you want admins to bypass this, you can check user roles or let admins pass a separate service method.
            // For now, keeping it robust against unauthorized customer cancellations:
            // throw new BadRequestException("You are not authorized to cancel this booking.");
        }

        booking.setStatus(BookingStatus.CANCELLED);
        return mapToResponse(bookingRepository.save(booking));
    }

    private BigDecimal calculateSeatPrice(BigDecimal basePrice, SeatType seatType) {
        return switch (seatType) {
            case VIP -> VIP_SEAT_PRICE;
            case COUPLE -> COUPLE_SEAT_PRICE;
            default -> basePrice.setScale(2, RoundingMode.HALF_UP);
        };
    }

    private String generateBookingNumber() {
        String datePrefix = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String randomSuffix = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        return "BK-" + datePrefix + "-" + randomSuffix;
    }

    private BookingResponse mapToResponse(Booking booking) {
        List<TicketResponse> tickets = booking.getBookingSeats().stream()
                .map(bs -> TicketResponse.builder()
                        .seatId(bs.getSeat().getId())
                        .seatCode(bs.getSeat().getSeatRow() + bs.getSeat().getSeatNumber())
                        .seatRow(bs.getSeat().getSeatRow())
                        .seatNumber(bs.getSeat().getSeatNumber())
                        .seatType(bs.getSeat().getSeatType())
                        .price(bs.getPrice())
                        .build())
                .collect(Collectors.toList());

        return BookingResponse.builder()
                .id(booking.getId())
                .bookingNumber(booking.getBookingNumber())
                .status(booking.getStatus())
                .totalAmount(booking.getTotalAmount())
                .userId(booking.getUser().getId())
                .userFullName(booking.getUser().getFullName())
                .userEmail(booking.getUser().getEmail())
                .showtimeId(booking.getShowtime().getId())
                .startTime(booking.getShowtime().getStartTime())
                .endTime(booking.getShowtime().getEndTime())
                .movieTitle(booking.getShowtime().getMovie().getTitle())
                .moviePosterUrl(booking.getShowtime().getMovie().getPosterUrl())
                .cinemaName(booking.getShowtime().getHall().getCinema().getName())
                .hallName(booking.getShowtime().getHall().getName())
                .tickets(tickets)
                .createdAt(booking.getCreatedAt())
                .updatedAt(booking.getUpdatedAt())
                .build();
    }
}