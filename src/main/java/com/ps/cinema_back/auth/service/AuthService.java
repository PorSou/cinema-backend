package com.ps.cinema_back.auth.service;

import com.ps.cinema_back.auth.dto.request.*;
import com.ps.cinema_back.auth.dto.response.AuthResponse;

public interface AuthService {

    void register(RegisterRequest request);

    AuthResponse verifyOtp(VerifyOtpRequest request);

    AuthResponse login(LoginRequest request);

    void resendOtp(String email);

    void forgotPassword(ForgotPasswordRequest request);

    void resetPassword(ResetPasswordRequest request);

    AuthResponse refreshToken(RefreshTokenRequest request);
}