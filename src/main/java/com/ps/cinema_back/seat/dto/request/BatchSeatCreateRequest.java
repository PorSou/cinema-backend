package com.ps.cinema_back.seat.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BatchSeatCreateRequest {

    @NotNull(message = "Hall ID is required")
    private Long hallId;

    // Empty list is allowed so it can wipe/clear hall layouts cleanly
    @Builder.Default
    private List<SeatRequest> seats = new ArrayList<>();
}