package com.ps.cinema_back.voucher.controller;

import com.ps.cinema_back.common.controller.BaseController;
import com.ps.cinema_back.common.response.ApiResponse;
import com.ps.cinema_back.voucher.dto.request.VoucherApplyRequest;
import com.ps.cinema_back.voucher.dto.response.VoucherApplyResponse;
import com.ps.cinema_back.voucher.service.VoucherService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/vouchers")
@RequiredArgsConstructor
@Tag(name = "Customer Vouchers", description = "Endpoints for validating and applying promo codes at checkout")
public class CustomerVoucherController extends BaseController {

    private final VoucherService voucherService;

    @PostMapping("/apply")
    @Operation(summary = "Validate and compute discount for a promo code against cart subtotal")
    public ResponseEntity<ApiResponse<VoucherApplyResponse>> applyVoucher(
            @Valid @RequestBody VoucherApplyRequest request) {
        VoucherApplyResponse response = voucherService.validateAndApplyVoucher(request.getCode(), request.getSubtotal());
        return OK(response, "Voucher applied successfully");
    }
}