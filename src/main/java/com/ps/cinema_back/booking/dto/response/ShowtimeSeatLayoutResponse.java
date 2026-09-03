package com.ps.cinema_back.booking.dto.response;

import com.ps.cinema_back.common.enums.SeatType;
import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShowtimeSeatLayoutResponse {
    private Long showtimeId;
    private Long hallId;
    private String hallName;
    private Integer totalSeats;
    private List<SeatAvailabilityResponse> seats;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SeatAvailabilityResponse {
        private Long seatId;
        private String seatCode;
        private String seatRow;
        private Integer seatNumber;
        private SeatType seatType;
        private Integer gridX;
        private Integer gridY;
        private BigDecimal calculatedPrice;
        private boolean isAvailable;
    }
}