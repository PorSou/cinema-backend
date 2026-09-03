package com.ps.cinema_back.seat.entity;

import com.ps.cinema_back.common.entity.AuditEntity;
import com.ps.cinema_back.common.enums.SeatType;
import com.ps.cinema_back.hall.entity.Hall;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "seats", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"hall_id", "seat_row", "seat_number"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Seat extends AuditEntity {

    @Column(name = "seat_row", nullable = false, length = 5)
    private String seatRow; // e.g. "A", "B", "C"

    @Column(name = "seat_number", nullable = false)
    private Integer seatNumber; // e.g. 1, 2, 3

    @Enumerated(EnumType.STRING)
    @Column(name = "seat_type", nullable = false)
    private SeatType seatType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hall_id", nullable = false)
    private Hall hall;

    // NEW: Store visual layout coordinates
    @Column(name = "grid_x")
    private Integer gridX;

    @Column(name = "grid_y")
    private Integer gridY;

    @Builder.Default
    @Column(name = "is_deleted", nullable = false)
    private Boolean isDeleted = false;
}