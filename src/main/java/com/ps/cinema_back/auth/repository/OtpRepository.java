package com.ps.cinema_back.auth.repository;

import com.ps.cinema_back.auth.entity.Otp;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OtpRepository extends JpaRepository<Otp, Long> {
    Optional<Otp> findTopByUserEmailAndIsUsedFalseOrderByIdDesc(String email);
    Optional<Otp> findTopByUserIdAndIsUsedFalseOrderByIdDesc(Long userId);
}