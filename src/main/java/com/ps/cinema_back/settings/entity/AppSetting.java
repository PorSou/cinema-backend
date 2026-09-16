package com.ps.cinema_back.settings.entity;

import com.ps.cinema_back.common.entity.AuditEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "app_settings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AppSetting extends AuditEntity {

    @Column(name = "setting_key", unique = true, nullable = false)
    private String key; // e.g., "SITE_LOGO_URL", "SITE_NAME"

    @Column(name = "setting_value", columnDefinition = "TEXT")
    private String value;
}