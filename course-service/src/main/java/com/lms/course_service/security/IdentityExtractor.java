package com.lms.course_service.security;

import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;

@Component
public class IdentityExtractor {

    private final JwtService jwtService;

    public IdentityExtractor(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    public RequestIdentity extract(HttpServletRequest request) {
        String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new SecurityException("Missing or invalid Authorization header");
        }

        String token = authHeader.substring(7);
        Claims claims = jwtService.parseToken(token);

        String userId = claims.getSubject();
        if (userId == null || userId.isBlank()) {
            throw new SecurityException("Missing user ID in token");
        }

        String roles = claims.get("roles", String.class);
        String email = claims.get("email", String.class);

        return new RequestIdentity(userId, roles, email);
    }
}
