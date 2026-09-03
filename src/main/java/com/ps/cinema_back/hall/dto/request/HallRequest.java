package com.ps.cinema_back.hall.dto.request;

import com.ps.cinema_back.common.enums.HallType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class HallRequest {

    @NotBlank(message = "Hall name is required (e.g. Hall 1, IMAX Hall)")
    private String name;

    @NotNull(message = "Hall type is required")
    private HallType hallType;

    @NotNull(message = "Cinema ID is required")
    private Long cinemaId;
}
