package com.ps.cinema_back.payment.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class KhqrGenerateRequest {

    @NotNull(message = "Booking ID is required")
    private Long bookingId;

    private String currency = "USD"; // Default USD
}