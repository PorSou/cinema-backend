package com.ps.cinema_back.concession.dto.response;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CategoryResponse {
    private Long id;
    private String name;
    private String description;
    private int itemCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}