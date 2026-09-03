package com.ps.cinema_back.booking.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TicketCheckInResponse {
    private String bookingNumber;
    private String status;
    private String movieTitle;
    private String cinemaName;
    private String hallName;
    private LocalDateTime showtime;
    private List<String> seatNumbers;
    private String customerName;
    private String customerEmail;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime checkedInAt;
    private String checkedInBy;
    private String message;
}