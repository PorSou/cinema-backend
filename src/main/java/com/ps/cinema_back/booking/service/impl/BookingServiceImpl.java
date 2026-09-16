package com.ps.cinema_back.booking.service.impl;

import com.ps.cinema_back.booking.dto.request.BookingRequest;
import com.ps.cinema_back.booking.dto.request.ConcessionSelectionRequest;
import com.ps.cinema_back.booking.dto.response.BookingConcessionResponse;
import com.ps.cinema_back.booking.dto.response.BookingResponse;
import com.ps.cinema_back.booking.dto.response.ShowtimeSeatLayoutResponse;
import com.ps.cinema_back.booking.dto.response.TicketResponse;
import com.ps.cinema_back.booking.entity.Booking;
import com.ps.cinema_back.booking.entity.BookingSeat;
import com.ps.cinema_back.booking.repository.BookingRepository;
import com.ps.cinema_back.booking.service.BookingService;
import com.ps.cinema_back.common.enums.BookingStatus;
import com.ps.cinema_back.common.enums.SeatAvailabilityStatus;
import com.ps.cinema_back.common.enums.SeatType;
import com.ps.cinema_back.common.exception.BadRequestException;
import com.ps.cinema_back.common.exception.ConflictException;
import com.ps.cinema_back.common.exception.ResourceNotFoundException;
import com.ps.cinema_back.concession.entity.BookingConcessionItem;
import com.ps.cinema_back.concession.entity.ConcessionItem;
import com.ps.cinema_back.concession.repository.BookingConcessionItemRepository;
import com.ps.cinema_back.concession.repository.ConcessionItemRepository;
import com.ps.cinema_back.notification.service.NotificationService; // 🌟 NEW — Notification Service
import com.ps.cinema_back.seat.entity.Seat;
import com.ps.cinema_back.seat.repository.SeatRepository;
import com.ps.cinema_back.showtime.entity.Showtime;
import com.ps.cinema_back.showtime.repository.ShowtimeRepository;
import com.ps.cinema_back.user.entity.User;
import com.ps.cinema_back.user.repository.UserRepository;
import com.ps.cinema_back.voucher.dto.response.VoucherApplyResponse;
import com.ps.cinema_back.voucher.service.VoucherService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
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

    private final ConcessionItemRepository concessionItemRepository;
    private final BookingConcessionItemRepository bookingConcessionItemRepository;

    private final VoucherService voucherService;
    private final NotificationService notificationService; // 🌟 NEW — Injected for real notifications

    private static final List<BookingStatus> ACTIVE_STATUSES = List.of(
            BookingStatus.PENDING,
            BookingStatus.CONFIRMED,
            BookingStatus.CHECKED_IN
    );

    // Couple/Twin seats are sold as a single flat-price pair ($10.00 for the pair, $5.00 per record)
    private static final BigDecimal COUPLE_PAIR_PRICE = new BigDecimal("10.00");
    private static final BigDecimal COUPLE_SEAT_PRICE =
            COUPLE_PAIR_PRICE.divide(BigDecimal.valueOf(2), 2, RoundingMode.HALF_UP);

    // VIP seats are a flat $10.00 each
    private static final BigDecimal VIP_SEAT_PRICE = new BigDecimal("10.00");

    @Override
    @Transactional
    public BookingResponse createBooking(Long currentUserId, BookingRequest request) {
        return createBooking(currentUserId, request, null);
    }

    @Override
    @Transactional
    public BookingResponse createBooking(Long currentUserId, BookingRequest request, String voucherCode) {
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

        // Validate concession selections BEFORE creating anything
        List<ConcessionSelectionRequest> concessionRequests =
                request.getConcessions() != null ? request.getConcessions() : List.of();

        Map<Long, ConcessionItem> resolvedConcessionItems = new HashMap<>();
        for (ConcessionSelectionRequest selection : concessionRequests) {
            ConcessionItem item = concessionItemRepository
                    .findByIdAndIsDeletedFalse(selection.getConcessionItemId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Concession item not found with id: " + selection.getConcessionItemId()));

            if (!item.isActive()) {
                throw new BadRequestException("Concession item '" + item.getName() + "' is not currently available.");
            }

            resolvedConcessionItems.put(selection.getConcessionItemId(), item);
        }

        // Calculate pricing per seat and running subtotal
        BigDecimal subtotal = BigDecimal.ZERO;
        List<BookingSeat> bookingSeats = new ArrayList<>();

        Booking booking = Booking.builder()
                .bookingNumber(generateBookingNumber())
                .status(BookingStatus.PENDING)
                .user(user)
                .showtime(showtime)
                .totalAmount(BigDecimal.ZERO)
                .discountAmount(BigDecimal.ZERO)
                .isDeleted(false)
                .build();

        for (Seat seat : seats) {
            BigDecimal seatPrice = calculateSeatPrice(showtime.getBasePrice(), seat.getSeatType());
            subtotal = subtotal.add(seatPrice);

            BookingSeat bookingSeat = BookingSeat.builder()
                    .booking(booking)
                    .seat(seat)
                    .price(seatPrice)
                    .build();

            bookingSeats.add(bookingSeat);
        }

        // Add F&B cost into the total
        for (ConcessionSelectionRequest selection : concessionRequests) {
            ConcessionItem item = resolvedConcessionItems.get(selection.getConcessionItemId());
            BigDecimal lineTotal = item.getPrice().multiply(BigDecimal.valueOf(selection.getQuantity()));
            subtotal = subtotal.add(lineTotal);
        }

        BigDecimal finalTotal = subtotal;

        if (voucherCode != null && !voucherCode.isBlank()) {
            VoucherApplyResponse applied = voucherService.validateAndApplyVoucher(voucherCode, subtotal);

            booking.setDiscountAmount(applied.getDiscountAmount());
            booking.setVoucherCode(applied.getCode());
            finalTotal = applied.getFinalTotal();

            voucherService.redeemVoucher(applied.getCode());
        }

        booking.setTotalAmount(finalTotal);
        booking.setBookingSeats(bookingSeats);

        Booking savedBooking = bookingRepository.save(booking);

        // Persist each F&B selection as a BookingConcessionItem row.
        if (!concessionRequests.isEmpty()) {
            List<BookingConcessionItem> concessionItemsToSave = concessionRequests.stream()
                    .map(selection -> {
                        ConcessionItem item = resolvedConcessionItems.get(selection.getConcessionItemId());
                        BigDecimal unitPrice = item.getPrice();
                        BigDecimal itemSubtotal = unitPrice.multiply(BigDecimal.valueOf(selection.getQuantity()));

                        return BookingConcessionItem.builder()
                                .booking(savedBooking)
                                .concessionItem(item)
                                .quantity(selection.getQuantity())
                                .unitPrice(unitPrice)
                                .subtotal(itemSubtotal)
                                .build();
                    })
                    .collect(Collectors.toList());

            bookingConcessionItemRepository.saveAll(concessionItemsToSave);
        }

        // 🌟 NEW — Automatically trigger real notification for admins & staff upon successful booking creation
        try {
            notificationService.createNotification(
                    "New Ticket Booking",
                    "Customer " + user.getFullName() + " placed booking #" + savedBooking.getBookingNumber() + " for " + showtime.getMovie().getTitle(),
                    "BOOKING"
            );
        } catch (Exception e) {
            // Catch error silently so a notification failure never breaks customer checkout
        }

        return mapToResponse(savedBooking);
    }

    @Override
    @Transactional(readOnly = true)
    public ShowtimeSeatLayoutResponse getSeatAvailabilityForShowtime(Long showtimeId) {
        Showtime showtime = showtimeRepository.findByIdAndIsDeletedFalse(showtimeId)
                .orElseThrow(() -> new ResourceNotFoundException("Showtime not found with id: " + showtimeId));

        List<Seat> allSeats = seatRepository.findAllByHallIdAndIsDeletedFalseOrderBySeatRowAscSeatNumberAsc(showtime.getHall().getId());

        List<BookingSeat> activeBookingSeats =
                bookingRepository.findActiveBookingSeatsForShowtime(showtimeId, ACTIVE_STATUSES);

        Map<Long, BookingStatus> seatStatusMap = activeBookingSeats.stream()
                .collect(Collectors.toMap(
                        bs -> bs.getSeat().getId(),
                        bs -> bs.getBooking().getStatus(),
                        (existing, replacement) -> existing
                ));

        List<ShowtimeSeatLayoutResponse.SeatAvailabilityResponse> seatResponses = allSeats.stream()
                .map(seat -> {
                    BookingStatus bookingStatus = seatStatusMap.get(seat.getId());

                    SeatAvailabilityStatus availabilityStatus;
                    if (bookingStatus == null) {
                        availabilityStatus = SeatAvailabilityStatus.AVAILABLE;
                    } else if (bookingStatus == BookingStatus.PENDING) {
                        availabilityStatus = SeatAvailabilityStatus.RESERVED;
                    } else {
                        availabilityStatus = SeatAvailabilityStatus.BOOKED;
                    }

                    return ShowtimeSeatLayoutResponse.SeatAvailabilityResponse.builder()
                            .seatId(seat.getId())
                            .seatCode(seat.getSeatRow() + seat.getSeatNumber())
                            .seatRow(seat.getSeatRow())
                            .seatNumber(seat.getSeatNumber())
                            .seatType(seat.getSeatType())
                            .gridX(seat.getGridX())
                            .gridY(seat.getGridY())
                            .calculatedPrice(calculateSeatPrice(showtime.getBasePrice(), seat.getSeatType()))
                            .isAvailable(availabilityStatus == SeatAvailabilityStatus.AVAILABLE)
                            .availabilityStatus(availabilityStatus)
                            .build();
                })
                .collect(Collectors.toList());

        return ShowtimeSeatLayoutResponse.builder()
                .showtimeId(showtime.getId())
                .hallId(showtime.getHall().getId())
                .hallName(showtime.getHall().getName())
                .totalSeats(allSeats.size())
                .seats(seatResponses)
                .build();
    }

    @Scheduled(fixedRate = 60000) // runs every 60s
    @Transactional
    public void expireStalePendingBookings() {
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(15); // hold window
        List<Booking> stale = bookingRepository
                .findAllByStatusAndCreatedAtBefore(BookingStatus.PENDING, cutoff);

        for (Booking booking : stale) {
            booking.setStatus(BookingStatus.CANCELLED);
        }
        bookingRepository.saveAll(stale);
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

        booking.setStatus(BookingStatus.CANCELLED);
        return mapToResponse(bookingRepository.save(booking));
    }

    @Override
    @Transactional
    public BookingResponse confirmCashBooking(Long bookingId) {
        Booking booking = bookingRepository.findByIdAndIsDeletedFalse(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with id: " + bookingId));

        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new BadRequestException("Only pending bookings can be confirmed.");
        }

        booking.setStatus(BookingStatus.CONFIRMED);

        // 🌟 NEW — Trigger notification for confirmed cash bookings too
        try {
            notificationService.createNotification(
                    "Booking Confirmed",
                    "Booking #" + booking.getBookingNumber() + " has been confirmed.",
                    "BOOKING"
            );
        } catch (Exception ignored) {}

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

        List<BookingConcessionResponse> concessions =
                bookingConcessionItemRepository.findByBookingId(booking.getId()).stream()
                        .map(bc -> BookingConcessionResponse.builder()
                                .concessionItemId(bc.getConcessionItem().getId())
                                .itemName(bc.getConcessionItem().getName())
                                .quantity(bc.getQuantity())
                                .unitPrice(bc.getUnitPrice())
                                .totalPrice(bc.getSubtotal())
                                .build())
                        .collect(Collectors.toList());

        return BookingResponse.builder()
                .id(booking.getId())
                .bookingNumber(booking.getBookingNumber())
                .status(booking.getStatus())
                .totalAmount(booking.getTotalAmount())
                .discountAmount(booking.getDiscountAmount())
                .voucherCode(booking.getVoucherCode())
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
                .concessions(concessions)
                .createdAt(booking.getCreatedAt())
                .updatedAt(booking.getUpdatedAt())
                .build();
    }
}