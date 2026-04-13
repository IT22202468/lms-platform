package com.lms.auth_service.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    /** HS256 requires a key at least 256 bits (32 bytes). */
    private static final String SECRET = "0123456789abcdef0123456789abcdef";

    @Test
    void generateAccessToken_containsSubjectEmailRolesAndExpiration() {
        JwtService jwtService = new JwtService(SECRET, 60L);

        String token = jwtService.generateAccessToken("user-1", "a@b.com", "STUDENT,INSTRUCTOR");

        SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
        var payload = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        assertThat(payload.getSubject()).isEqualTo("user-1");
        assertThat(payload.get("email", String.class)).isEqualTo("a@b.com");
        assertThat(payload.get("roles", String.class)).isEqualTo("STUDENT,INSTRUCTOR");
        Date iat = payload.getIssuedAt();
        Date exp = payload.getExpiration();
        assertThat(iat).isNotNull();
        assertThat(exp).isNotNull();
        assertThat(exp.toInstant()).isAfter(iat.toInstant());
    }
}
