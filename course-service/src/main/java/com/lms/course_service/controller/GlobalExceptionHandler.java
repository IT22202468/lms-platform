package com.lms.course_service.controller;

import com.lms.course_service.exception.CourseNotFoundException;
import com.lms.course_service.exception.UnauthorizedException;
import com.lms.course_service.exception.ValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;

import java.util.List;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(CourseNotFoundException.class)
    public ResponseEntity<?> handleCourseNotFound(CourseNotFoundException ex) {
        logClientError(ex, HttpStatus.NOT_FOUND, "not_found", "failure");
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("message", ex.getMessage()));
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<?> handleUnauthorized(UnauthorizedException ex) {
        logClientError(ex, HttpStatus.FORBIDDEN, "authorization", "forbidden");
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of("message", ex.getMessage()));
    }

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<?> handleValidation(ValidationException ex) {
        logClientError(ex, HttpStatus.BAD_REQUEST, "validation", "failure");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("message", ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<?> handleMethodArgumentNotValid(MethodArgumentNotValidException ex) {
        List<String> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .toList();
        logClientError(ex, HttpStatus.BAD_REQUEST, "validation", "failure");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("message", "Validation failed", "errors", errors));
    }

    @ExceptionHandler(SecurityException.class)
    public ResponseEntity<?> handleSecurity(SecurityException ex) {
        String msg = ex.getMessage() == null ? "Forbidden" : ex.getMessage();

        if (msg.toLowerCase().contains("missing")
                || msg.toLowerCase().contains("gateway")) {
            logClientError(ex, HttpStatus.UNAUTHORIZED, "security", "unauthorized");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", msg));
        }

        logClientError(ex, HttpStatus.FORBIDDEN, "security", "forbidden");
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of("message", msg));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<?> handleMaxUpload(MaxUploadSizeExceededException ex) {
        logClientError(ex, HttpStatus.PAYLOAD_TOO_LARGE, "payload_too_large", "failure");
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
                .body(Map.of("message", "Uploaded file exceeds the maximum allowed size"));
    }

    @ExceptionHandler(MultipartException.class)
    public ResponseEntity<?> handleMultipart(MultipartException ex) {
        logClientError(ex, HttpStatus.BAD_REQUEST, "multipart", "failure");
        String msg = ex.getMessage() != null ? ex.getMessage() : "Invalid or incomplete multipart request";
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("message", msg));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<?> handleBadRequest(IllegalArgumentException ex) {
        logClientError(ex, HttpStatus.BAD_REQUEST, "bad_request", "client");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("message", ex.getMessage() == null ? "Bad request" : ex.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handleUnexpected(Exception ex) {
        log.atError()
                .addKeyValue("event.action", "http_error_response")
                .addKeyValue("event.outcome", "server_error")
                .addKeyValue("error.type", ex.getClass().getName())
                .addKeyValue("error.category", "internal_error")
                .addKeyValue("http.response.status_code", HttpStatus.INTERNAL_SERVER_ERROR.value())
                .setCause(ex)
                .log("Unhandled exception");

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("message", "Internal server error"));
    }

    private static void logClientError(
            Exception ex,
            HttpStatus status,
            String errorCategory,
            String outcome
    ) {
        log.atWarn()
                .addKeyValue("event.action", "http_error_response")
                .addKeyValue("event.outcome", outcome)
                .addKeyValue("error.type", ex.getClass().getName())
                .addKeyValue("error.category", errorCategory)
                .addKeyValue("http.response.status_code", status.value())
                .log(ex.getMessage() != null ? ex.getMessage() : errorCategory);
    }
}
