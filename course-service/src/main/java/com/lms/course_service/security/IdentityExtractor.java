package com.lms.course_service.security;

import jakarta.servlet.http.HttpServletRequest;

public class IdentityExtractor {

    public RequestIdentity extract(HttpServletRequest request){
        String userId = request.getHeader("X-User-Id");
        String roles = request.getHeader("X-User-Roles");

        if(userId == null || userId.isBlank()){
            throw new SecurityException("Missing X-User-Id");
        }
        return new RequestIdentity(userId, roles);
    }
}
