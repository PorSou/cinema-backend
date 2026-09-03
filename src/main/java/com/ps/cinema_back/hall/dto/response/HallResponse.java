package com.ps.cinema_back.hall.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.ps.cinema_back.common.enums.HallType;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class HallResponse {
    private Long id;
    private String name;
    private HallType hallType;
    private Integer totalSeats;
    private Long cinemaId;
    private String cinemaName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
