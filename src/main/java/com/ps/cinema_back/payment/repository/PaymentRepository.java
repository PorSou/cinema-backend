package com.ps.cinema_back.payment.repository;

import com.ps.cinema_back.payment.entity.Payment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findByIdAndIsDeletedFalse(Long id);
    Optional<Payment> findByTransactionIdAndIsDeletedFalse(String transactionId);
    Optional<Payment> findByBookingIdAndIsDeletedFalse(Long bookingId);
    Page<Payment> findAllByIsDeletedFalse(Pageable pageable);
}