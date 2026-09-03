package com.ps.cinema_back.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@MappedSuperclass
public abstract class SoftDeleteEntity extends AuditEntity {

    @Column(name = "is_deleted", nullable = false)
    private Boolean isDeleted = false;
}