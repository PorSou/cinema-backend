package com.ps.cinema_back.notification.entity;

import com.ps.cinema_back.common.entity.AuditEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "notifications")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification extends AuditEntity {

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, length = 500)
    private String message;

    @Column(nullable = false)
    private String type; // e.g., BOOKING, PAYMENT, SYSTEM

    @Column(nullable = false)
    @Builder.Default
    private Boolean isRead = false;

}