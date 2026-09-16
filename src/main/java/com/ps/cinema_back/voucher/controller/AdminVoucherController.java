package com.ps.cinema_back.voucher.controller;

import com.ps.cinema_back.common.controller.BaseController;
import com.ps.cinema_back.common.response.ApiResponse;
import com.ps.cinema_back.common.response.PageResponse;
import com.ps.cinema_back.voucher.dto.request.VoucherRequest;
import com.ps.cinema_back.voucher.dto.response.VoucherResponse;
import com.ps.cinema_back.voucher.service.VoucherService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/vouchers")
@RequiredArgsConstructor
@Tag(name = "Admin Promotional Vouchers", description = "Full admin CRUD and analytics tracking for promo codes")
@PreAuthorize("hasRole('ADMIN')")
public class AdminVoucherController extends BaseController {

    private final VoucherService voucherService;

    @PostMapping
    @Operation(summary = "Create a new promotional discount voucher")
    public ResponseEntity<ApiResponse<VoucherResponse>> createVoucher(@Valid @RequestBody VoucherRequest request) {
        return OK(voucherService.createVoucher(request), "Voucher created successfully");
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an existing promotional voucher")
    public ResponseEntity<ApiResponse<VoucherResponse>> updateVoucher(
            @PathVariable Long id,
            @Valid @RequestBody VoucherRequest request) {
        return OK(voucherService.updateVoucher(id, request), "Voucher updated successfully");
    }

    @PatchMapping("/{id}/toggle-status")
    @Operation(summary = "Instant active/inactive status toggle for a voucher campaign")
    public ResponseEntity<ApiResponse<VoucherResponse>> toggleStatus(@PathVariable Long id) {
        return OK(voucherService.toggleVoucherStatus(id), "Voucher status toggled successfully");
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete (soft-delete) a promotional voucher")
    public ResponseEntity<ApiResponse<Void>> deleteVoucher(@PathVariable Long id) {
        voucherService.deleteVoucher(id);
        return OK(null, "Voucher deleted successfully");
    }

    @GetMapping
    @Operation(summary = "Get all vouchers with usage tracking statistics")
    public ResponseEntity<ApiResponse<PageResponse<VoucherResponse>>> getAllVouchers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return OK(PageResponse.of(voucherService.getAllVouchers(pageable)), "Vouchers fetched successfully");
    }
}