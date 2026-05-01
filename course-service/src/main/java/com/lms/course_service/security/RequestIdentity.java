package com.lms.course_service.security;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class RequestIdentity {

    private final String userId;
    private final Set<String> roles;
    private final String email;

    public RequestIdentity(String userId, String rolesHeader) {
        this(userId, rolesHeader, null);
    }

    public RequestIdentity(String userId, String rolesHeader, String email) {
        this.userId = userId;
        this.roles = parseRoles(rolesHeader);
        this.email = email;
    }

    private Set<String> parseRoles(String rolesHeader){
        if (rolesHeader == null || rolesHeader.isBlank()) return Set.of();
        return new HashSet<>(Arrays.stream(rolesHeader.split(","))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .map(String::toUpperCase)
                .toList());
    }

    public String getUserId() { return userId; }

    /**
     * Email from gateway header X-User-Email (JWT claim); may be null in tests.
     */
    public String getEmail() { return email; }

    public boolean hasRole(String role) { return roles.contains(role.toUpperCase()); }
}
