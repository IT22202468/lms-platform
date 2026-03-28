package com.lms.course_service.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(SecurityException.class)
    public ResponseEntity<?> handleSecurity(SecurityException ex) {
        String msg = ex.getMessage() == null ? "Forbidden" : ex.getMessage();

        if (msg.toLowerCase().contains("missing x-user-id")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", msg));
        }
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", msg));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<?> handleBadRequest(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("message", ex.getMessage() == null ? "Bad request" : ex.getMessage()));
    }
}
