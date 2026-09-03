package com.ps.cinema_back.auth.service;

import com.ps.cinema_back.auth.entity.Otp;
import com.ps.cinema_back.auth.repository.OtpRepository;
import com.ps.cinema_back.common.exception.BadRequestException;
import com.ps.cinema_back.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class OtpService {

    private final OtpRepository otpRepository;
    private final EmailService emailService;

    @Transactional
    public void generateAndSendOtp(User user) {
        String code = String.format("%06d", new SecureRandom().nextInt(1000000));

        Otp otp = Otp.builder()
                .user(user)
                .email(user.getEmail()) // 👈 Sets the email required by PostgreSQL
                .code(code)
                .expiredAt(LocalDateTime.now().plusMinutes(5))
                .isUsed(false)
                .build();

        otpRepository.save(otp);

        emailService.sendOtpEmail(user.getEmail(), code);
    }

    @Transactional
    public User validateAndConsumeOtp(String email, String code) {
        Otp otp = otpRepository.findTopByUserEmailAndIsUsedFalseOrderByIdDesc(email)
                .orElseThrow(() -> new BadRequestException("Invalid or expired OTP code"));

        if (!otp.getCode().equals(code)) {
            throw new BadRequestException("Invalid OTP code");
        }

        if (otp.isExpired()) {
            throw new BadRequestException("OTP code has expired. Please request a new code.");
        }

        otp.setIsUsed(true);
        otpRepository.save(otp);

        return otp.getUser();
    }
}