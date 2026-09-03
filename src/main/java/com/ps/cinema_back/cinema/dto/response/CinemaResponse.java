package com.ps.cinema_back.cinema.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CinemaResponse {
    private Long id;
    private String name;
    private String city;
    private String address; // 👈 Added address
    private String phone;   // 👈 Added phone
    private Integer totalHalls;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
