package com.ps.cinema_back.cinema.entity;

import com.ps.cinema_back.common.entity.AuditEntity;
import com.ps.cinema_back.hall.entity.Hall;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "cinemas")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Cinema extends AuditEntity {

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String city;

    @Column(nullable = false)
    private String address; // 👈 Added address

    @Column(nullable = false, length = 20)
    private String phone; // 👈 Added phone

    @Column(name = "total_halls", nullable = false)
    @Builder.Default
    private Integer totalHalls = 0;

    @OneToMany(mappedBy = "cinema", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<Hall> halls = new ArrayList<>();

    @Builder.Default
    @Column(name = "is_deleted", nullable = false)
    private Boolean isDeleted = false;
}