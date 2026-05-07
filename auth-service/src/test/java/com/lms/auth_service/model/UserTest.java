package com.lms.auth_service.model;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class UserTest {

    @Test
    void constructorAndAccessorsWork() {
        Set<String> roles = Set.of("STUDENT", "INSTRUCTOR");
        User user = new User("user@example.com", "hash", roles);
        Instant createdAt = Instant.parse("2026-05-07T00:00:00Z");

        user.setId("u1");
        user.setCreatedAt(createdAt);

        assertThat(user.getId()).isEqualTo("u1");
        assertThat(user.getEmail()).isEqualTo("user@example.com");
        assertThat(user.getPasswordHash()).isEqualTo("hash");
        assertThat(user.getRoles()).isEqualTo(roles);
        assertThat(user.getCreatedAt()).isEqualTo(createdAt);
    }

    @Test
    void defaultConstructorAndSettersWork() {
        User user = new User();
        Instant createdAt = Instant.parse("2026-05-07T12:00:00Z");

        user.setId("u2");
        user.setEmail("other@example.com");
        user.setPasswordHash("secret");
        user.setRoles(Set.of("STUDENT"));
        user.setCreatedAt(createdAt);

        assertThat(user.getId()).isEqualTo("u2");
        assertThat(user.getEmail()).isEqualTo("other@example.com");
        assertThat(user.getPasswordHash()).isEqualTo("secret");
        assertThat(user.getRoles()).containsExactly("STUDENT");
        assertThat(user.getCreatedAt()).isEqualTo(createdAt);
    }
}