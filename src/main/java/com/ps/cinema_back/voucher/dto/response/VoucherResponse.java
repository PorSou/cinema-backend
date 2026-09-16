package com.ps.cinema_back.voucher.dto.response;

import com.ps.cinema_back.common.enums.DiscountType;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VoucherResponse {
    private Long id;
    private String code;
    private DiscountType discountType;
    private BigDecimal discountValue;
    private BigDecimal minSpend;
    private LocalDateTime expiryDate;
    private Integer usageLimit;
    private Integer timesUsed; // Usage tracking analytics
    private Boolean isActive;
    private LocalDateTime createdAt;
}