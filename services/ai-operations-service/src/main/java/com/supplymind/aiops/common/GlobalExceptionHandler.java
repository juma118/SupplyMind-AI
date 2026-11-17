package com.supplymind.aiops.common;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(TenantRequiredException.class)
    public ResponseEntity<ApiError> handleTenantRequired(TenantRequiredException ex, HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage(), request);
    }

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(NotFoundException ex, HttpServletRequest request) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage(), request);
    }

    @ExceptionHandler(InvalidStateTransitionException.class)
    public ResponseEntity<ApiError> handleInvalidTransition(InvalidStateTransitionException ex, HttpServletRequest request) {
        return build(HttpStatus.CONFLICT, ex.getMessage(), request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .reduce((a, b) -> a + "; " + b)
                .orElse("Validation failed");
        return build(HttpStatus.BAD_REQUEST, message, request);
    }

    private ResponseEntity<ApiError> build(HttpStatus status, String message, HttpServletRequest request) {
        ApiError body = ApiError.of(status.value(), status.getReasonPhrase(), message, request.getRequestURI());
        return ResponseEntity.status(status).body(body);
    }
}
