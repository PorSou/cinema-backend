package com.ps.cinema_back.seat.dto.request;

import com.ps.cinema_back.common.enums.SeatType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SeatRequest {

    @NotBlank(message = "Seat row is required (e.g. A, B, C)")
    private String seatRow;

    @NotNull(message = "Seat number is required")
    @Min(value = 1, message = "Seat number must be at least 1")
    private Integer seatNumber;

    @NotNull(message = "Seat type is required")
    private SeatType seatType;

    @NotNull(message = "Hall ID is required")
    private Long hallId;

    private Integer gridX;
    private Integer gridY;
}
