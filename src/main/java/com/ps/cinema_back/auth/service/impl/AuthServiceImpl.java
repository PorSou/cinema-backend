package com.ps.cinema_back.auth.service.impl;

import com.ps.cinema_back.auth.dto.request.*;
import com.ps.cinema_back.auth.dto.response.AuthResponse;
import com.ps.cinema_back.auth.service.AuthService;
import com.ps.cinema_back.auth.service.OtpService;
import com.ps.cinema_back.auth.service.TurnstileService; // <--- Import TurnstileService
import com.ps.cinema_back.common.enums.Role;
import com.ps.cinema_back.common.exception.BadRequestException;
import com.ps.cinema_back.common.exception.ConflictException;
import com.ps.cinema_back.common.exception.ResourceNotFoundException;
import com.ps.cinema_back.security.JwtUtils;
import com.ps.cinema_back.security.KeycloakTokenService;
import com.ps.cinema_back.user.dto.response.UserResponse;
import com.ps.cinema_back.user.entity.User;
import com.ps.cinema_back.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final OtpService otpService;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;
    private final TurnstileService turnstileService; // <--- Inject TurnstileService

    private final KeycloakTokenService keycloakTokenService; // add to fields (RequiredArgsConstructor picks it up)

    @Override
    @Transactional
    public AuthResponse loginWithKeycloak(KeycloakLoginRequest request) {
        org.springframework.security.oauth2.jwt.Jwt jwt =
                keycloakTokenService.verifyAndDecode(request.getAccessToken());

        String email = jwt.getClaimAsString("email");
        String fullName = jwt.getClaimAsString("name");

        if (email == null || email.isBlank()) {
            throw new BadRequestException("Social login did not provide an email address.");
        }

        // Track whether we're creating a brand-new account or reusing an
        // existing one, so the frontend can show the right message.
        boolean[] isNewUser = {false};

        User user = userRepository.findByEmailAndIsDeletedFalse(email)
                .orElseGet(() -> {
                    isNewUser[0] = true;
                    User newUser = User.builder()
                            .fullName(fullName != null ? fullName : email)
                            .email(email)
                            .password(null)
                            .role(Role.CUSTOMER)
                            .isActive(true)
                            .build();
                    return userRepository.save(newUser);
                });

        if (!Boolean.TRUE.equals(user.getIsActive())) {
            user.setIsActive(true);
            userRepository.save(user);
        }

        String accessToken = jwtUtils.generateAccessToken(user.getEmail(), user.getRole().name());
        String refreshToken = jwtUtils.generateRefreshToken(user.getEmail());

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .user(mapToUserResponse(user))
                .isNewUser(isNewUser[0])   // 👈 new field
                .build();
    }

    @Override
    @Transactional
    public void register(RegisterRequest request) {
        // 1. Verify Cloudflare Turnstile token first
        boolean isHuman = turnstileService.verifyToken(request.getTurnstileToken());
        if (!isHuman) {
            throw new BadRequestException("Cloudflare verification failed. Please complete the human check.");
        }

        // 2. Check if email already exists (app-level check — catches the
        // common case cheaply without ever touching the database's unique
        // constraint).
        if (userRepository.existsByEmailAndIsDeletedFalse(request.getEmail())) {
            throw new ConflictException("Email is already registered: " + request.getEmail());
        }

        User user = User.builder()
                .fullName(request.getFullName())
                .email(request.getEmail())
                .phone(request.getPhone())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(Role.CUSTOMER)
                .isActive(false)
                .build();

        // 👇 NEW: safety net for the case the app-level check above misses —
        // e.g. a row with this email still exists but is soft-deleted
        // (isDeleted = true), so existsByEmailAndIsDeletedFalse() returns
        // false even though the DB's unique constraint on `email` still
        // blocks the insert. Without this, that scenario surfaces as a raw
        // 500 "Unhandled exception" with a full Hibernate/Postgres stack
        // trace instead of a clean, expected 409 response.
        try {
            User savedUser = userRepository.save(user);
            otpService.generateAndSendOtp(savedUser);
        } catch (DataIntegrityViolationException e) {
            throw new ConflictException("Email is already registered: " + request.getEmail());
        }
    }

    @Override
    @Transactional
    public AuthResponse verifyOtp(VerifyOtpRequest request) {
        User user = otpService.validateAndConsumeOtp(request.getEmail(), request.getCode());
        user.setIsActive(true);
        userRepository.save(user);

        String accessToken = jwtUtils.generateAccessToken(user.getEmail(), user.getRole().name());
        String refreshToken = jwtUtils.generateRefreshToken(user.getEmail());

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .user(mapToUserResponse(user))
                .build();
    }

    @Override
    @Transactional
    public void resendOtp(String email) {
        User user = userRepository.findByEmailAndIsDeletedFalse(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));

        if (Boolean.TRUE.equals(user.getIsActive())) {
            throw new BadRequestException("Account is already active and verified.");
        }

        otpService.generateAndSendOtp(user);
    }

    @Override
    @Transactional
    public void forgotPassword(ForgotPasswordRequest request) {
        User user = userRepository.findByEmailAndIsDeletedFalse(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + request.getEmail()));

        otpService.generateAndSendOtp(user);
    }

    @Override
    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        User user = otpService.validateAndConsumeOtp(request.getEmail(), request.getCode());
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }

    @Override
    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {

        // 1. Verify Cloudflare Turnstile token first
        boolean isHuman = turnstileService.verifyToken(request.getTurnstileToken());
        if (!isHuman) {
            throw new BadRequestException("Cloudflare verification failed. Please complete the human check.");
        }

        // 2. Existing login validation logic
        User user = userRepository.findByEmailAndIsDeletedFalse(request.getEmail())
                .orElseThrow(() -> new BadRequestException("Invalid email or password"));

        // 👇 FIXED: this null check now runs BEFORE passwordEncoder.matches().
        // A social-login-only account (Google/GitHub/Facebook) has
        // password = null. Calling passwordEncoder.matches(raw, null)
        // throws a NullPointerException — which is NOT a BadRequestException,
        // so it was falling through to the generic 500 handler instead of
        // showing this friendly message. Checking null first avoids ever
        // calling matches() with a null encoded password.
        if (user.getPassword() == null) {
            throw new BadRequestException(
                    "This account uses social login. Please sign in with Google, GitHub, or Facebook instead."
            );
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BadRequestException("Invalid email or password");
        }

        if (!Boolean.TRUE.equals(user.getIsActive())) {
            throw new BadRequestException("Account is not verified yet. Please verify your OTP code.");
        }

        String accessToken = jwtUtils.generateAccessToken(user.getEmail(), user.getRole().name());
        String refreshToken = jwtUtils.generateRefreshToken(user.getEmail());

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .user(mapToUserResponse(user))
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public AuthResponse refreshToken(RefreshTokenRequest request) {
        String refreshToken = request.getRefreshToken();

        if (!jwtUtils.validateToken(refreshToken)) {
            throw new BadRequestException("Invalid or expired refresh token");
        }

        String email = jwtUtils.getEmailFromToken(refreshToken);
        User user = userRepository.findByEmailAndIsDeletedFalse(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));

        if (!Boolean.TRUE.equals(user.getIsActive())) {
            throw new BadRequestException("User account is inactive");
        }

        String newAccessToken = jwtUtils.generateAccessToken(user.getEmail(), user.getRole().name());

        return AuthResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(refreshToken)
                .user(mapToUserResponse(user))
                .build();
    }

    private UserResponse mapToUserResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .phone(user.getPhone())
                .role(user.getRole())
                .isActive(user.getIsActive())
                .createdAt(user.getCreatedAt())
                .build();
    }
}