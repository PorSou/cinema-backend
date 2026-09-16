package com.ps.cinema_back.common.exception;


import com.ps.cinema_back.common.response.ApiBody;
import com.ps.cinema_back.common.response.ApiResponse;
import com.ps.cinema_back.common.response.ApiStatus;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.apache.catalina.connector.ClientAbortException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.async.AsyncRequestNotUsableException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // ----------------------------------------------------
    // CLIENT DISCONNECT EXCEPTIONS
    // ----------------------------------------------------

    // 0. Client (browser) closed the connection or navigated away before
    // the response finished writing — e.g. page refresh, tab close, or a
    // cancelled poll request. This is normal, expected behavior, NOT a
    // bug. There is nobody left to send a response to, so we just log
    // quietly at DEBUG level instead of letting it surface as an ERROR
    // stack trace via the generic handler below.
    //
    // IMPORTANT: this must be declared before handleGeneric(Exception.class)
    // is checked — Spring matches the MOST SPECIFIC exception type first
    // regardless of method order, so this works correctly either way, but
    // keeping it at the top makes the intent clear.
    @ExceptionHandler({ClientAbortException.class, AsyncRequestNotUsableException.class})
    public void handleClientAbort(Exception ex) {
        log.debug("Client disconnected before response completed: {}", ex.getMessage());
        // Intentionally no response body — the client is already gone,
        // and attempting to write one would just throw again.
    }

    // ----------------------------------------------------
    // CUSTOM APPLICATION EXCEPTIONS
    // ----------------------------------------------------

    // 1. 404 NOT FOUND (Resource not found in DB)
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<Object>> handleNotFound(ResourceNotFoundException ex) {
        return buildErrorResponse(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    // 2. 409 CONFLICT (Duplicate email / unique constraint failure)
    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ApiResponse<Object>> handleConflict(ConflictException ex) {
        return buildErrorResponse(HttpStatus.CONFLICT, ex.getMessage());
    }

    // 3. 400 BAD REQUEST (Business logic errors)
    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ApiResponse<Object>> handleBadRequest(BadRequestException ex) {
        return buildErrorResponse(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    // ----------------------------------------------------
    // SECURITY & AUTHENTICATION EXCEPTIONS
    // ----------------------------------------------------

    // 4. 401 UNAUTHORIZED (Invalid login credentials / bad JWT)
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiResponse<Object>> handleBadCredentials(BadCredentialsException ex) {
        return buildErrorResponse(HttpStatus.UNAUTHORIZED, "Invalid email or password");
    }

    // 5. 403 FORBIDDEN (Insufficient permissions / roles)
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Object>> handleAccessDenied(AccessDeniedException ex) {
        return buildErrorResponse(HttpStatus.FORBIDDEN, "You don't have permission to perform this action");
    }

    // ----------------------------------------------------
    // SPRING WEB & VALIDATION EXCEPTIONS
    // ----------------------------------------------------

    // 6. 400 BAD REQUEST (@Valid DTO field violations)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(fe ->
                errors.put(fe.getField(), fe.getDefaultMessage())
        );

        ApiResponse<Map<String, String>> response = ApiResponse.<Map<String, String>>builder()
                .success(false)
                .body(ApiBody.<Map<String, String>>builder()
                        .data(errors)
                        .build())
                .status(ApiStatus.builder()
                        .code(HttpStatus.BAD_REQUEST.value())
                        .message("Validation Failed")
                        .build())
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    // 7. 400 BAD REQUEST (Query / path constraint violations)
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Object>> handleConstraintViolation(ConstraintViolationException ex) {
        return buildErrorResponse(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    // 8. 400 BAD REQUEST (Malformed JSON body)
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Object>> handleHttpMessageNotReadable(HttpMessageNotReadableException ex) {
        return buildErrorResponse(HttpStatus.BAD_REQUEST, "Malformed JSON request body or invalid data types");
    }

    // 9. 400 BAD REQUEST (Invalid parameter data type, e.g., passing string for ID)
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Object>> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        String message = String.format("Parameter '%s' should be of type '%s'",
                ex.getName(),
                ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "unknown");
        return buildErrorResponse(HttpStatus.BAD_REQUEST, message);
    }

    // 10. 400 BAD REQUEST (Missing required request parameter)
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiResponse<Object>> handleMissingParam(MissingServletRequestParameterException ex) {
        return buildErrorResponse(HttpStatus.BAD_REQUEST, "Required request parameter '" + ex.getParameterName() + "' is missing");
    }

    // 11. 404 NOT FOUND (Invalid route/URL hit)
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiResponse<Object>> handleNoResourceFound(NoResourceFoundException ex) {
        return buildErrorResponse(HttpStatus.NOT_FOUND, "The requested endpoint path does not exist");
    }

    // 12. 405 METHOD NOT ALLOWED (e.g. GET instead of POST)
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiResponse<Object>> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex) {
        return buildErrorResponse(HttpStatus.METHOD_NOT_ALLOWED, "HTTP method " + ex.getMethod() + " is not supported for this endpoint");
    }

    // 13. 415 UNSUPPORTED MEDIA TYPE (e.g. Sending text/plain instead of application/json)
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ApiResponse<Object>> handleMediaTypeNotSupported(HttpMediaTypeNotSupportedException ex) {
        return buildErrorResponse(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Content type '" + ex.getContentType() + "' is not supported");
    }

    // 👇 NEW — 14. 409 CONFLICT (raw DB unique-constraint violation that
    // slipped through without being caught locally in a service method).
    // This is a safety net: your registration flow already catches this
    // itself and rethrows as ConflictException, so this handler mainly
    // protects OTHER services that insert/update rows with unique
    // constraints (e.g. a future feature) from ever surfacing a raw
    // Hibernate/Postgres stack trace as a scary 500 "Unhandled exception".
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Object>> handleDataIntegrityViolation(DataIntegrityViolationException ex) {
        log.warn("Data integrity violation: {}", ex.getMostSpecificCause().getMessage());
        return buildErrorResponse(
                HttpStatus.CONFLICT,
                "This record conflicts with existing data. Please check your input and try again."
        );
    }

    // ----------------------------------------------------
    // CATCH-ALL UNHANDLED SERVER ERRORS
    // ----------------------------------------------------

    // 15. 500 INTERNAL SERVER ERROR
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Object>> handleGeneric(Exception ex) {
        // If a plain IOException bubbles up here and is ALSO a client
        // disconnect (some environments throw IOException directly
        // instead of wrapping it as ClientAbortException), don't log it
        // as a full server error either.
        if (ex instanceof IOException && isClientAbort(ex)) {
            log.debug("Client disconnected before response completed: {}", ex.getMessage());
            return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Client disconnected");
        }

        log.error("Unhandled exception", ex); // Keep logged for server debugging
        return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "An internal server error occurred. Please try again later.");
    }

    @ExceptionHandler(BakongAuthException.class)
    public ResponseEntity<ApiResponse<Object>> handleBakongAuth(BakongAuthException ex) {
        log.error("Bakong authentication failed: {}", ex.getMessage());
        return buildErrorResponse(HttpStatus.SERVICE_UNAVAILABLE, ex.getMessage());
    }

    private boolean isClientAbort(Throwable ex) {
        String msg = ex.getMessage();
        return msg != null && (
                msg.contains("Broken pipe") ||
                        msg.contains("Connection reset") ||
                        msg.contains("aborted by the software")
        );
    }

    // ----------------------------------------------------
    // HELPER BUILDER METHOD
    // ----------------------------------------------------
    private ResponseEntity<ApiResponse<Object>> buildErrorResponse(HttpStatus status, String message) {
        ApiResponse<Object> response = ApiResponse.builder()
                .success(false)
                .status(ApiStatus.builder()
                        .code(status.value())
                        .message(message)
                        .build())
                .build();

        return ResponseEntity.status(status).body(response);
    }
}