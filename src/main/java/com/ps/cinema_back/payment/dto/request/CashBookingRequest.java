package com.ps.cinema_back.payment.dto.request;

import com.ps.cinema_back.booking.dto.request.ConcessionSelectionRequest;
import lombok.Data;

import java.util.List;
import java.util.Set;

@Data
public class CashBookingRequest {
    private Long showtimeId;
    private Set<Long> seatIds;

    private List<ConcessionSelectionRequest> concessions;

    // 👇 NEW — optional voucher code applied at the counter/checkout.
    // Only this cash-booking flow accepts a voucher for now; other
    // request DTOs (BookingRequest, PaymentRequest) are left untouched
    // so the KHQR / generic booking flows keep behaving exactly as before.
    private String voucherCode;

}