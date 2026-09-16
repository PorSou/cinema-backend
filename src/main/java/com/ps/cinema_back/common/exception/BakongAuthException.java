package com.ps.cinema_back.common.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
public class BakongAuthException extends RuntimeException {

    public BakongAuthException(String message) {
        super(message);
    }
}