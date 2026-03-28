package com.lms.course_service.security;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class RequestIdentity {

    private final String userId;
    private final Set<String> roles;

    public RequestIdentity(String userId, String rolesHeader) {
        this.userId = userId;
        this.roles = parseRoles(rolesHeader);
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
    public boolean hasRole(String role) { return roles.contains(role.toUpperCase()); }
}
