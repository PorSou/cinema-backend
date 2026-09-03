package com.ps.cinema_back.seat.dto.request;

import com.ps.cinema_back.common.enums.SeatType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BulkSeatGenerateRequest {

    @NotNull(message = "Hall ID is required")
    private Long hallId;

    @NotBlank(message = "Start row is required (e.g. A)")
    private String startRow; // e.g. "A"

    @NotBlank(message = "End row is required (e.g. H)")
    private String endRow; // e.g. "H"

    @NotNull(message = "Seats per row is required")
    @Min(value = 1, message = "There must be at least 1 seat per row")
    private Integer seatsPerRow; // e.g. 10

    @NotNull(message = "Default seat type is required")
    private SeatType seatType; // e.g. REGULAR or VIP
}
