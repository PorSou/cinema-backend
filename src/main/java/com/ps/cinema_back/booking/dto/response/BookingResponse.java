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

    // This is now the FINAL amount charged (subtotal minus discountAmount,
    // when a voucher was applied). Previously it was always the raw
    // pre-discount subtotal, which is why the ticket page and Telegram
    // notification always showed the full price even after a voucher
    // was applied at checkout.
    private BigDecimal totalAmount;

    // 👇 NEW — how much the voucher knocked off, and which code was used.
    // The ticket detail page's frontend already checks
    // `discountAmount > 0` to render the "Subtotal" / "Voucher Discount"
    // rows, so populating this is all that page needed.
    private BigDecimal discountAmount;
    private String voucherCode;

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

    // F&B pre-order lines for this booking.
    private List<BookingConcessionResponse> concessions;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}