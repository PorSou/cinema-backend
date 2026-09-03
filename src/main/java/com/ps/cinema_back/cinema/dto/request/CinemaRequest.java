package com.ps.cinema_back.cinema.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CinemaRequest {

    @NotBlank(message = "Cinema name is required")
    private String name;

    @NotBlank(message = "City is required")
    private String city;

    @NotBlank(message = "Address is required")
    private String address; // 👈 Added address validation

    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^[+0-9\\s\\-\\(\\)]{8,20}$", message = "Invalid phone number format")
    private String phone; // 👈 Added phone validation
}
