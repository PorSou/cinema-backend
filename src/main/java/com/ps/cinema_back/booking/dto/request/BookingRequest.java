package com.ps.cinema_back.booking.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Set;

@Getter
@Setter
public class BookingRequest {

    @NotNull(message = "Showtime ID is required")
    private Long showtimeId;

    @NotEmpty(message = "At least one seat must be selected")
    private Set<Long> seatIds;

    // Optional — a cash/counter booking may not include any F&B pre-order
    private List<ConcessionSelectionRequest> concessions;
}
