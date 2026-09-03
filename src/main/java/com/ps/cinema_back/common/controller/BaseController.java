package com.ps.cinema_back.common.controller;


import com.ps.cinema_back.common.response.ApiResponse;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;

public abstract class BaseController {

//    protected <T> ResponseEntity<ApiResponse<T>> OK() {
//        return ResponseEntity.ok(ApiResponse.success(null, HttpStatus.OK, "ok"));
//    }

    protected <T> ResponseEntity<ApiResponse<T>> OK(T data) {
        return ResponseEntity.ok(ApiResponse.success(data, HttpStatus.OK, "ok"));
    }

    protected <T> ResponseEntity<ApiResponse<T>> OK(T data, String message) {
        return ResponseEntity.ok(ApiResponse.success(data, HttpStatus.OK, message));
    }

    protected <T> ResponseEntity<ApiResponse<T>> CREATED(T data) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(data, HttpStatus.CREATED, "Created successfully"));
    }

    protected <T> ResponseEntity<ApiResponse<T>> CREATED(T data, String message) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(data, HttpStatus.CREATED, message));
    }

    protected <T> ResponseEntity<ApiResponse<List<T>>> PAGE(Page<T> page) {
        return ResponseEntity.ok(ApiResponse.success(page, HttpStatus.OK, "ok"));
    }

    protected <T> ResponseEntity<ApiResponse<List<T>>> PAGE(Page<T> page, String message) {
        return ResponseEntity.ok(ApiResponse.success(page, HttpStatus.OK, message));
    }
}