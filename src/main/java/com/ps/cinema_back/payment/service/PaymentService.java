package com.ps.cinema_back.payment.service;

import com.ps.cinema_back.payment.dto.request.CashBookingRequest;
import com.ps.cinema_back.payment.dto.request.KhqrGenerateRequest;
import com.ps.cinema_back.payment.dto.request.PaymentRequest;
import com.ps.cinema_back.payment.dto.response.BakongCheckMd5Response;
import com.ps.cinema_back.payment.dto.response.PaymentResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface PaymentService {
    PaymentResponse bookAndPayCash(Long userId, CashBookingRequest request);
    PaymentResponse processCashPayment(Long currentUserId, PaymentRequest request);
    PaymentResponse generateKhqr(Long currentUserId, KhqrGenerateRequest request);
    PaymentResponse verifyBakongPayment(String transactionId);
    PaymentResponse getPaymentById(Long id);
    PaymentResponse getPaymentByTransactionId(String transactionId);
    PaymentResponse getPaymentByBookingId(Long bookingId);
    Page<PaymentResponse> getAllPayments(Pageable pageable);
    PaymentResponse refundPayment(Long id);

    BakongCheckMd5Response checkPaymentStatus(String transactionId);
}

