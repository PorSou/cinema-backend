package com.ps.cinema_back.auth.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;

import com.ps.cinema_back.user.dto.response.UserResponse;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AuthResponse {

    private String accessToken;
    private String refreshToken;
    @Builder.Default
    private String tokenType = "Bearer";
    private UserResponse user;
}