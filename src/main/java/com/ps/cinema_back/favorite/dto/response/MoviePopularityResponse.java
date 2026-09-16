package com.ps.cinema_back.favorite.dto.response;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MoviePopularityResponse {
    private Long movieId;
    private String movieTitle;
    private String moviePosterUrl;
    private Long watchlistCount;
}