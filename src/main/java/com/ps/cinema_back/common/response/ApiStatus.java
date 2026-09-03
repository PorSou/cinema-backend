package com.ps.cinema_back.common.response;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApiStatus {

    private int code;
    private String message;
}
