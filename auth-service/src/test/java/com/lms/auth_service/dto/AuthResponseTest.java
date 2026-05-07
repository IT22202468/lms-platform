package com.lms.auth_service.dto;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AuthResponseTest {

    @Test
    void constructorSetsAccessTokenAndDefaultTokenType() {
        AuthResponse response = new AuthResponse("jwt-token");

        assertThat(response.getAccessToken()).isEqualTo("jwt-token");
        assertThat(response.getTokenType()).isEqualTo("Bearer");
    }

    @Test
    void settersWork() {
        AuthResponse response = new AuthResponse();

        response.setAccessToken("abc");
        response.setTokenType("Token");

        assertThat(response.getAccessToken()).isEqualTo("abc");
        assertThat(response.getTokenType()).isEqualTo("Token");
    }
}