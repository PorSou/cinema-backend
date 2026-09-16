package com.ps.cinema_back.voucher.entity;

import com.ps.cinema_back.common.entity.AuditEntity;
import com.ps.cinema_back.common.enums.DiscountType;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "promotional_vouchers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Voucher extends AuditEntity {

    @NotBlank
    @Column(unique = true, nullable = false)
    private String code; // e.g. CINE-SUMMER20

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DiscountType discountType;

    @NotNull
    @Column(nullable = false)
    private BigDecimal discountValue; // e.g. 15.00 (for 15%) or 3.00 (for $3)

    @Builder.Default
    @Column(nullable = false)
    private BigDecimal minSpend = BigDecimal.ZERO; // Minimum cart total required

    private LocalDateTime expiryDate;

    @Builder.Default
    @Column(nullable = false)
    private Integer usageLimit = 100; // Max number of times this voucher can be used globally

    @Builder.Default
    @Column(nullable = false)
    private Integer timesUsed = 0; // Tracks how many times customers redeemed it

    @Builder.Default
    @Column(nullable = false)
    private Boolean isActive = true; // Instant active/inactive toggle

    @Builder.Default
    @Column(nullable = false)
    private Boolean isDeleted = false;
}