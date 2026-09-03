package com.ps.cinema_back.seat.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.ps.cinema_back.common.enums.SeatType;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SeatResponse {
    private Long id;
    private String seatCode; // e.g. "A1", "A2", "B5"
    private String seatRow;
    private Integer seatNumber;
    private SeatType seatType;
    private Integer gridX;
    private Integer gridY;
    private Long hallId;
    private String hallName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
