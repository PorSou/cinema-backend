package com.ps.cinema_back.showtime.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.ps.cinema_back.common.enums.HallType;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ShowtimeResponse {

    private Long id;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endTime;

    private BigDecimal basePrice;

    // Movie details
    private Long movieId;
    private String movieTitle;
    private Integer movieDurationMinutes;
    private String moviePosterUrl;

    // Hall & Cinema details
    private Long hallId;
    private String hallName;
    private HallType hallType;
    private Long cinemaId;
    private String cinemaName;
    private String cinemaCity;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}