package com.ps.cinema_back.booking.repository;

import com.ps.cinema_back.booking.entity.Booking;
import com.ps.cinema_back.booking.entity.BookingSeat;
import com.ps.cinema_back.common.enums.BookingStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {

    List<Booking> findAllByStatusAndCreatedAtBefore(BookingStatus status, LocalDateTime cutoff);

    // 👇 Add this method to check if any active bookings are tied to a showtime
    boolean existsByShowtimeIdAndIsDeletedFalse(Long showtimeId);

    Optional<Booking> findByIdAndIsDeletedFalse(Long id);

    Optional<Booking> findByBookingNumberAndIsDeletedFalse(String bookingNumber);

    Page<Booking> findAllByUserIdAndIsDeletedFalse(Long userId, Pageable pageable);

    Page<Booking> findAllByIsDeletedFalse(Pageable pageable);

    // Checks if any requested seats are already booked for this showtime (PENDING or CONFIRMED)
    @Query("""
        SELECT bs.seat.id FROM BookingSeat bs
        WHERE bs.booking.showtime.id = :showtimeId
          AND bs.booking.status IN (:activeStatuses)
          AND bs.booking.isDeleted = false
          AND bs.seat.id IN (:seatIds)
    """)
    List<Long> findAlreadyBookedSeatIds(
            @Param("showtimeId") Long showtimeId,
            @Param("activeStatuses") List<BookingStatus> activeStatuses,
            @Param("seatIds") Set<Long> seatIds
    );

    // Returns all booked seat IDs for a showtime to show seat availability layout
    @Query("""
        SELECT bs.seat.id FROM BookingSeat bs
        WHERE bs.booking.showtime.id = :showtimeId
          AND bs.booking.status IN (:activeStatuses)
          AND bs.booking.isDeleted = false
    """)
    Set<Long> findAllBookedSeatIdsForShowtime(
            @Param("showtimeId") Long showtimeId,
            @Param("activeStatuses") List<BookingStatus> activeStatuses
    );

    List<Booking> findAllByStatusAndCreatedAtBeforeAndIsDeletedFalse(BookingStatus status, LocalDateTime threshold);

    // Returns the active BookingSeat rows for a showtime, so we can tell
// RESERVED (PENDING) apart from BOOKED (CONFIRMED/CHECKED_IN) per seat.
    @Query("""
    SELECT bs FROM BookingSeat bs
    WHERE bs.booking.showtime.id = :showtimeId
      AND bs.booking.status IN (:activeStatuses)
      AND bs.booking.isDeleted = false
""")
    List<BookingSeat> findActiveBookingSeatsForShowtime(
            @Param("showtimeId") Long showtimeId,
            @Param("activeStatuses") List<BookingStatus> activeStatuses
    );
}