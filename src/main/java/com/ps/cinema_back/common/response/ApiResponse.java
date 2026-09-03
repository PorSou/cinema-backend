package com.ps.cinema_back.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    private Boolean success;
    private ApiBody<T> body;
    private ApiStatus status;

    // Factory method for single objects or un-paged lists
    public static <T> ApiResponse<T> success(T data, HttpStatus status, String message) {
        return ApiResponse.<T>builder()
                .success(true)
                .body(ApiBody.<T>builder()
                        .data(data)
                        .build())
                .status(ApiStatus.builder()
                        .code(status.value())
                        .message(message)
                        .build())
                .build();
    }

    // Factory method for Spring Data Page results
    public static <T> ApiResponse<List<T>> success(Page<T> page, HttpStatus status, String message) {
        return ApiResponse.<List<T>>builder()
                .success(true)
                .body(ApiBody.<List<T>>builder()
                        .data(page.getContent())
                        .page(PageMetaData.from(page))
                        .build())
                .status(ApiStatus.builder()
                        .code(status.value())
                        .message(message)
                        .build())
                .build();
    }
}