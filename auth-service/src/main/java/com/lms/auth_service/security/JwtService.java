package com.lms.auth_service.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Map;

@Service
public class JwtService {

    private final SecretKey key;

    private final long accessTokenMinutes;

    public JwtService(
            @Value("${security.jwt.secret}") String secret,
            @Value("${security.jwt.access-token-minutes}") long accessTokenMinutes
            ) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTokenMinutes = accessTokenMinutes;
    }

    public String generateAccessToken(String userId, String email, String rolesCsv){
        Instant now= Instant.now();
        Instant exp= now.plusSeconds(accessTokenMinutes * 60);

        return Jwts.builder()
                .subject(userId)
                .claims(Map.of(
                        "email", email,
                        "roles", rolesCsv
                ))
                .issuedAt(Date.from(now))
                .expiration(Date.from(exp))
                .signWith(key)
                .compact();
    }
}
