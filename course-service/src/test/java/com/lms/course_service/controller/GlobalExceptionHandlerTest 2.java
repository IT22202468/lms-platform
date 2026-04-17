package com.lms.course_service.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
    }

    @Test
    void handleSecurity_missingInMessage_returns401() {
        ResponseEntity<?> r = handler.handleSecurity(new SecurityException("Missing X-User-Id"));

        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(r.getBody()).isInstanceOf(Map.class);
        @SuppressWarnings("unchecked")
        Map<String, String> body = (Map<String, String>) r.getBody();
        assertThat(body.get("message")).containsIgnoringCase("missing");
    }

    @Test
    void handleSecurity_gatewayInMessage_returns401() {
        ResponseEntity<?> r = handler.handleSecurity(new SecurityException("Request must come through gateway"));

        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void handleSecurity_otherMessage_returns403() {
        ResponseEntity<?> r = handler.handleSecurity(new SecurityException("Not your course"));

        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        @SuppressWarnings("unchecked")
        Map<String, String> body = (Map<String, String>) r.getBody();
        assertThat(body.get("message")).isEqualTo("Not your course");
    }

    @Test
    void handleSecurity_nullMessage_usesForbiddenLabel() {
        ResponseEntity<?> r = handler.handleSecurity(new SecurityException());

        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void handleBadRequest_returns400WithMessage() {
        ResponseEntity<?> r = handler.handleBadRequest(new IllegalArgumentException("bad"));

        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        @SuppressWarnings("unchecked")
        Map<String, String> body = (Map<String, String>) r.getBody();
        assertThat(body.get("message")).isEqualTo("bad");
    }

    @Test
    void handleBadRequest_nullMessage_usesDefault() {
        ResponseEntity<?> r = handler.handleBadRequest(new IllegalArgumentException());

        @SuppressWarnings("unchecked")
        Map<String, String> body = (Map<String, String>) r.getBody();
        assertThat(body.get("message")).isEqualTo("Bad request");
    }
}
