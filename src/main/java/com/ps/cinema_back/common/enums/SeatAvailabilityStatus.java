package com.ps.cinema_back.common.enums;

public enum SeatAvailabilityStatus {
    AVAILABLE,
    RESERVED, // someone has a PENDING booking on it (mid-checkout, not yet paid)
    BOOKED    // CONFIRMED or CHECKED_IN — permanently taken for this showtime
}