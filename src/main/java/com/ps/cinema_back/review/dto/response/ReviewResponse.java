package com.ps.cinema_back.review.dto.response;

import lombok.*;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReviewResponse {
    private Long id;
    private Long userId;
    private String userFullName;
    private Long movieId;
    private String movieTitle;
    private Integer rating;
    private String comment;
    private Boolean isHidden;
    private LocalDateTime createdAt;
}