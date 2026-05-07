package com.lms.api_gateway.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private static final String SECRET = "0123456789abcdef0123456789abcdef";

    @Test
    void parseToken_returnsClaimsSignedWithSameSecret() {
        SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
        String token = Jwts.builder()
                .subject("uid-1")
                .claims(Map.of("email", "x@y.com", "roles", "STUDENT"))
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 60_000))
                .signWith(key)
                .compact();

        JwtService jwtService = new JwtService(SECRET);
        var claims = jwtService.parseToken(token);

        assertThat(claims.getSubject()).isEqualTo("uid-1");
        assertThat(claims.get("email", String.class)).isEqualTo("x@y.com");
        assertThat(claims.get("roles", String.class)).isEqualTo("STUDENT");
    }

    @Test
    void parseToken_malformed_throws() {
        JwtService jwtService = new JwtService(SECRET);

        assertThatThrownBy(() -> jwtService.parseToken("not-a-jwt"))
                .isInstanceOf(Exception.class);
    }

    @Test
    void parseToken_wrongSignature_throws() {
        SecretKey otherKey = Keys.hmacShaKeyFor("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa".getBytes(StandardCharsets.UTF_8));
        String token = Jwts.builder()
                .subject("uid-1")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 60_000))
                .signWith(otherKey)
                .compact();

        JwtService jwtService = new JwtService(SECRET);

        assertThatThrownBy(() -> jwtService.parseToken(token))
                .isInstanceOf(Exception.class);
    }

        @Test
        void parseToken_expired_throws() {
        SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
        String token = Jwts.builder()
            .subject("uid-1")
            .claims(Map.of("email", "x@y.com", "roles", "STUDENT"))
            .issuedAt(new Date(System.currentTimeMillis() - 120_000))
            .expiration(new Date(System.currentTimeMillis() - 60_000))
            .signWith(key)
            .compact();

        JwtService jwtService = new JwtService(SECRET);

        assertThatThrownBy(() -> jwtService.parseToken(token))
            .isInstanceOf(Exception.class);
        }

        @Test
        void parseToken_emptyToken_throws() {
        JwtService jwtService = new JwtService(SECRET);

        assertThatThrownBy(() -> jwtService.parseToken(""))
            .isInstanceOf(Exception.class);
        }
}
