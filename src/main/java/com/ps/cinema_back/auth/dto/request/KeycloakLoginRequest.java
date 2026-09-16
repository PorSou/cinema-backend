package com.ps.cinema_back.auth.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class KeycloakLoginRequest {
    @NotBlank
    private String accessToken; // the token Keycloak issued to the frontend
}