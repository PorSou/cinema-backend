package com.ps.cinema_back.voucher.service;

import com.ps.cinema_back.voucher.dto.request.VoucherRequest;
import com.ps.cinema_back.voucher.dto.response.VoucherApplyResponse;
import com.ps.cinema_back.voucher.dto.response.VoucherResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;

public interface VoucherService {
    VoucherResponse createVoucher(VoucherRequest request);
    VoucherResponse updateVoucher(Long id, VoucherRequest request);
    VoucherResponse toggleVoucherStatus(Long id);
    void deleteVoucher(Long id);
    Page<VoucherResponse> getAllVouchers(Pageable pageable);

    // Preview-only: validates the code and returns what the discount WOULD
    // be for the given subtotal. Does NOT increment usage — safe to call
    // repeatedly from a checkout page's "Apply" button.
    VoucherApplyResponse validateAndApplyVoucher(String code, BigDecimal subtotal);

    // 👇 NEW: actually commits the redemption — increments timesUsed by one.
    // Call this exactly once, at the point a booking is actually created
    // with the voucher applied (not on every "Apply" click), otherwise
    // usageLimit becomes unenforceable.
    void redeemVoucher(String code);
}