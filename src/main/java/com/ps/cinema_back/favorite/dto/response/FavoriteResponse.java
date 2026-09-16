package com.ps.cinema_back.favorite.dto.response;

import lombok.*;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FavoriteResponse {
    private Long id;
    private Long movieId;
    private String movieTitle;
    private String moviePosterUrl;
    private String movieGenre;
    private Integer movieDurationMinutes;
    private LocalDateTime createdAt;
}