package com.ps.cinema_back.favorite.entity;

import com.ps.cinema_back.common.entity.AuditEntity;
import com.ps.cinema_back.movie.entity.Movie;
import com.ps.cinema_back.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "user_favorites", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"user_id", "movie_id"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Favorite extends AuditEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "movie_id", nullable = false)
    private Movie movie;

    @Builder.Default
    @Column(nullable = false)
    private Boolean isDeleted = false;
}