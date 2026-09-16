package com.ps.cinema_back.audit.entity;

import com.ps.cinema_back.common.entity.AuditEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "audit_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLog extends AuditEntity {

    @Column(nullable = false)
    private String userEmail;

    @Column(nullable = false)
    private String action; // e.g., CREATE_MOVIE, DELETE_SHOWTIME, UPDATE_VOUCHER

    @Column(columnDefinition = "TEXT")
    private String description;

    private String ipAddress;
}