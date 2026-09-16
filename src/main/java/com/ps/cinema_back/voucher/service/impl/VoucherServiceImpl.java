package com.ps.cinema_back.voucher.service.impl;

import com.ps.cinema_back.audit.service.AuditLogService;
import com.ps.cinema_back.common.enums.DiscountType;
import com.ps.cinema_back.common.exception.BadRequestException;
import com.ps.cinema_back.common.exception.ResourceNotFoundException;
import com.ps.cinema_back.voucher.dto.request.VoucherRequest;
import com.ps.cinema_back.voucher.dto.response.VoucherApplyResponse;
import com.ps.cinema_back.voucher.dto.response.VoucherResponse;
import com.ps.cinema_back.voucher.entity.Voucher;
import com.ps.cinema_back.voucher.repository.VoucherRepository;
import com.ps.cinema_back.voucher.service.VoucherService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class VoucherServiceImpl implements VoucherService {

    private final VoucherRepository voucherRepository;
    private final AuditLogService auditLogService;

    @Override
    @Transactional
    public VoucherResponse createVoucher(VoucherRequest request) {
        String upperCode = request.getCode().toUpperCase().trim();
        if (voucherRepository.existsByCodeAndIsDeletedFalse(upperCode)) {
            throw new BadRequestException("Voucher code already exists: " + upperCode);
        }

        Voucher voucher = Voucher.builder()
                .code(upperCode)
                .discountType(request.getDiscountType())
                .discountValue(request.getDiscountValue())
                .minSpend(request.getMinSpend() != null ? request.getMinSpend() : BigDecimal.ZERO)
                .expiryDate(request.getExpiryDate())
                .usageLimit(request.getUsageLimit() != null ? request.getUsageLimit() : 100)
                .isActive(true)
                .isDeleted(false)
                .build();

        Voucher savedVoucher = voucherRepository.save(voucher);

        auditLogService.logAction(
                "CREATE_VOUCHER",
                "Created promotional voucher code: '" + savedVoucher.getCode() + "' with discount value: " + savedVoucher.getDiscountValue()
        );

        return mapToResponse(savedVoucher);
    }

    @Override
    @Transactional
    public VoucherResponse updateVoucher(Long id, VoucherRequest request) {
        Voucher voucher = voucherRepository.findById(id)
                .filter(v -> !v.getIsDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Voucher not found with id: " + id));

        String oldCode = voucher.getCode();
        String upperCode = request.getCode().toUpperCase().trim();
        if (!voucher.getCode().equals(upperCode) && voucherRepository.existsByCodeAndIsDeletedFalse(upperCode)) {
            throw new BadRequestException("Voucher code already exists: " + upperCode);
        }

        voucher.setCode(upperCode);
        voucher.setDiscountType(request.getDiscountType());
        voucher.setDiscountValue(request.getDiscountValue());
        voucher.setMinSpend(request.getMinSpend() != null ? request.getMinSpend() : BigDecimal.ZERO);
        voucher.setExpiryDate(request.getExpiryDate());
        if (request.getUsageLimit() != null) {
            voucher.setUsageLimit(request.getUsageLimit());
        }

        Voucher updatedVoucher = voucherRepository.save(voucher);

        auditLogService.logAction(
                "UPDATE_VOUCHER",
                "Updated voucher ID " + id + " from code '" + oldCode + "' to '" + updatedVoucher.getCode() + "'"
        );

        return mapToResponse(updatedVoucher);
    }

    @Override
    @Transactional
    public VoucherResponse toggleVoucherStatus(Long id) {
        Voucher voucher = voucherRepository.findById(id)
                .filter(v -> !v.getIsDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Voucher not found with id: " + id));

        voucher.setIsActive(!voucher.getIsActive());
        Voucher updatedVoucher = voucherRepository.save(voucher);

        auditLogService.logAction(
                "TOGGLE_VOUCHER_STATUS",
                "Changed status of voucher '" + updatedVoucher.getCode() + "' (ID: " + id + ") to active = " + updatedVoucher.getIsActive()
        );

        return mapToResponse(updatedVoucher);
    }

    @Override
    @Transactional
    public void deleteVoucher(Long id) {
        Voucher voucher = voucherRepository.findById(id)
                .filter(v -> !v.getIsDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Voucher not found with id: " + id));

        voucher.setIsDeleted(true);
        voucherRepository.save(voucher);

        auditLogService.logAction(
                "DELETE_VOUCHER",
                "Deleted promotional voucher code: '" + voucher.getCode() + "' (ID: " + id + ")"
        );
    }

    @Override
    @Transactional(readOnly = true)
    public Page<VoucherResponse> getAllVouchers(Pageable pageable) {
        return voucherRepository.findAllByIsDeletedFalse(pageable).map(this::mapToResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public VoucherApplyResponse validateAndApplyVoucher(String code, BigDecimal subtotal) {
        Voucher voucher = voucherRepository.findByCodeAndIsDeletedFalse(code.toUpperCase().trim())
                .orElseThrow(() -> new ResourceNotFoundException("Invalid or non-existent promo code."));

        if (!Boolean.TRUE.equals(voucher.getIsActive())) {
            throw new BadRequestException("This promotional voucher is currently inactive.");
        }

        if (voucher.getExpiryDate() != null && LocalDateTime.now().isAfter(voucher.getExpiryDate())) {
            throw new BadRequestException("This promotional voucher has expired.");
        }

        if (voucher.getTimesUsed() >= voucher.getUsageLimit()) {
            throw new BadRequestException("This promotional voucher has reached its maximum global usage limit.");
        }

        if (subtotal.compareTo(voucher.getMinSpend()) < 0) {
            throw new BadRequestException(String.format("Minimum spend of $%s required to use this voucher.", voucher.getMinSpend()));
        }

        BigDecimal discountAmount;
        if (voucher.getDiscountType() == DiscountType.PERCENTAGE) {
            discountAmount = subtotal.multiply(voucher.getDiscountValue())
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        } else {
            discountAmount = voucher.getDiscountValue();
        }

        if (discountAmount.compareTo(subtotal) > 0) {
            discountAmount = subtotal;
        }

        BigDecimal finalTotal = subtotal.subtract(discountAmount).setScale(2, RoundingMode.HALF_UP);

        return VoucherApplyResponse.builder()
                .code(voucher.getCode())
                .originalTotal(subtotal)
                .discountAmount(discountAmount)
                .finalTotal(finalTotal)
                .build();
    }

    @Override
    @Transactional
    public void redeemVoucher(String code) {
        // 👇 NEW: this is the piece that was missing entirely before —
        // validateAndApplyVoucher only ever previewed the discount and
        // never touched timesUsed, so usageLimit was unenforceable no
        // matter how many times a code was actually used on a real booking.
        Voucher voucher = voucherRepository.findByCodeAndIsDeletedFalse(code.toUpperCase().trim())
                .orElseThrow(() -> new ResourceNotFoundException("Invalid or non-existent promo code."));

        if (voucher.getTimesUsed() >= voucher.getUsageLimit()) {
            throw new BadRequestException("This promotional voucher has reached its maximum global usage limit.");
        }

        voucher.setTimesUsed(voucher.getTimesUsed() + 1);
        voucherRepository.save(voucher);
    }

    private VoucherResponse mapToResponse(Voucher v) {
        return VoucherResponse.builder()
                .id(v.getId())
                .code(v.getCode())
                .discountType(v.getDiscountType())
                .discountValue(v.getDiscountValue())
                .minSpend(v.getMinSpend())
                .expiryDate(v.getExpiryDate())
                .usageLimit(v.getUsageLimit())
                .timesUsed(v.getTimesUsed())
                .isActive(v.getIsActive())
                .createdAt(v.getCreatedAt())
                .build();
    }
}