package com.ps.cinema_back.voucher.dto.request;

import com.ps.cinema_back.common.enums.DiscountType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class VoucherRequest {
    @NotBlank(message = "Voucher code is required")
    private String code;

    @NotNull(message = "Discount type is required")
    private DiscountType discountType;

    @NotNull(message = "Discount value is required")
    @Min(value = 0, message = "Discount value must be positive")
    private BigDecimal discountValue;

    private BigDecimal minSpend;

    private LocalDateTime expiryDate;

    @Min(value = 1, message = "Usage limit must be at least 1")
    private Integer usageLimit;
}