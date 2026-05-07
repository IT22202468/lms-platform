package com.lms.auth_service.controller;

import com.lms.auth_service.exception.UnauthorizedException;
import com.lms.auth_service.exception.ValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
    }

    @Test
    void handleUnauthorized_returns401() {
        ResponseEntity<?> response = handler.handleUnauthorized(new UnauthorizedException("Not allowed"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        @SuppressWarnings("unchecked")
        Map<String, String> body = (Map<String, String>) response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.get("message")).isEqualTo("Not allowed");
    }

    @Test
    void handleValidation_returns400() {
        ResponseEntity<?> response = handler.handleValidation(new ValidationException("Invalid input"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        @SuppressWarnings("unchecked")
        Map<String, String> body = (Map<String, String>) response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.get("message")).isEqualTo("Invalid input");
    }

    @Test
    void handleMethodArgumentNotValid_returns400WithErrors() {
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        BindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "request");
        bindingResult.addError(new FieldError("request", "email", "must be a well-formed email address"));
        bindingResult.addError(new FieldError("request", "password", "size must be between 8 and 120"));
        when(ex.getBindingResult()).thenReturn(bindingResult);

        ResponseEntity<?> response = handler.handleMethodArgumentNotValid(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.get("message")).isEqualTo("Validation failed");
        assertThat(body.get("errors")).asList().contains(
                "email: must be a well-formed email address",
                "password: size must be between 8 and 120"
        );
    }

    @Test
    void handleSecurity_missingKeyword_returns401() {
        ResponseEntity<?> response = handler.handleSecurity(new SecurityException("Missing X-User-Id"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void handleSecurity_otherMessage_returns403() {
        ResponseEntity<?> response = handler.handleSecurity(new SecurityException("Forbidden"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void handleSecurity_nullMessage_usesDefaultForbiddenMessage() {
        ResponseEntity<?> response = handler.handleSecurity(new SecurityException());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        @SuppressWarnings("unchecked")
        Map<String, String> body = (Map<String, String>) response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.get("message")).isEqualTo("Forbidden");
    }

    @Test
    void handleBadRequest_nullMessage_usesDefaultText() {
        ResponseEntity<?> response = handler.handleBadRequest(new IllegalArgumentException());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        @SuppressWarnings("unchecked")
        Map<String, String> body = (Map<String, String>) response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.get("message")).isEqualTo("Bad request");
    }

    @Test
    void handleUnexpected_returns500() {
        ResponseEntity<?> response = handler.handleUnexpected(new RuntimeException("boom"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        @SuppressWarnings("unchecked")
        Map<String, String> body = (Map<String, String>) response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.get("message")).isEqualTo("Internal server error");
    }
}