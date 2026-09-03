package com.ps.cinema_back.movie.dto.request;


import com.ps.cinema_back.common.enums.Language;
import com.ps.cinema_back.common.enums.MovieStatus;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.Set;

@Getter
@Setter
public class MovieRequest {

    @NotBlank(message = "Title is required")
    private String title;

    private String description;

    @NotNull(message = "Duration is required")
    @Min(value = 1, message = "Duration must be at least 1 minute")
    private Integer durationMinutes;

    @NotNull(message = "Status is required") // 👈 Changed from @NotBlank to @NotNull
    private MovieStatus status;

    @NotNull(message = "Language is required")
    private Language language;

    private String ageRating;

    private String trailerUrl;

    private LocalDate releaseDate;

    private Set<Long> genreIds;

    private MultipartFile posterFile;
}