package com.ps.cinema_back.booking.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.ps.cinema_back.common.enums.BookingStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BookingResponse {

    private Long id;
    private String bookingNumber;
    private BookingStatus status;
    private BigDecimal totalAmount;

    // Customer info
    private Long userId;
    private String userFullName;
    private String userEmail;

    // Showtime info
    private Long showtimeId;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endTime;

    // Movie info
    private String movieTitle;
    private String moviePosterUrl;

    // Cinema & Hall info
    private String cinemaName;
    private String hallName;

    // Reserved seats (Tickets)
    private List<TicketResponse> tickets;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}