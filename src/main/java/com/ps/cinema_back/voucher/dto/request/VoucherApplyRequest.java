package com.ps.cinema_back.voucher.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class VoucherApplyRequest {
    @NotBlank(message = "Voucher code is required")
    private String code;

    @NotNull(message = "Cart subtotal is required")
    private BigDecimal subtotal;
}