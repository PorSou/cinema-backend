package com.ps.cinema_back.auth.controller;

import com.ps.cinema_back.auth.dto.request.*;
import com.ps.cinema_back.auth.dto.response.AuthResponse;
import com.ps.cinema_back.auth.service.AuthService;
import com.ps.cinema_back.common.controller.BaseController;
import com.ps.cinema_back.common.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController extends BaseController {

    private final AuthService authService;

    @PostMapping("/keycloak")
    public ResponseEntity<ApiResponse<AuthResponse>> loginWithKeycloak(
            @Valid @RequestBody KeycloakLoginRequest request) {
        return OK(authService.loginWithKeycloak(request), "Logged in successfully via social login");
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<Void>> register(@Valid @RequestBody RegisterRequest request) {
        authService.register(request);
        return OK(null, "Registration successful. Please check your email for the OTP code.");
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<ApiResponse<AuthResponse>> verifyOtp(@Valid @RequestBody VerifyOtpRequest request) {
        return OK(authService.verifyOtp(request), "Account verified successfully");
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        // Automatically passes the LoginRequest (containing the turnstileToken) to AuthService
        return OK(authService.login(request), "Logged in successfully");
    }

    @PostMapping("/resend-otp")
    public ResponseEntity<ApiResponse<Void>> resendOtp(@RequestParam String email) {
        authService.resendOtp(email);
        return OK(null, "OTP code has been resent to your email.");
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<Void>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        authService.forgotPassword(request);
        return OK(null, "Password reset OTP has been sent to your email.");
    }

    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<Void>> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return OK(null, "Password has been reset successfully. You can now login.");
    }

    @PostMapping("/refresh-token")
    public ResponseEntity<ApiResponse<AuthResponse>> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        return OK(authService.refreshToken(request), "Token refreshed successfully");
    }
}