package com.ps.cinema_back.movie.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;

import com.ps.cinema_back.common.enums.Language;
import com.ps.cinema_back.common.enums.MovieStatus;
import com.ps.cinema_back.genre.dto.response.GenreResponse;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MovieResponse {

    private Long id;
    private String title;
    private String description;
    private Integer durationMinutes;
    private MovieStatus status; // 👈 Changed to Enum
    private Language language;
    private String ageRating;
    private String posterUrl;
    private String trailerUrl;
    private LocalDate releaseDate;
    private Set<GenreResponse> genres;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}