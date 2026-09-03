package com.ps.cinema_back.booking.dto.response;

import com.ps.cinema_back.common.enums.SeatType;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TicketResponse {
    private Long seatId;
    private String seatCode; // e.g. "A1"
    private String seatRow;
    private Integer seatNumber;
    private SeatType seatType;
    private BigDecimal price;
}