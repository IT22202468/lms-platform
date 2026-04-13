package com.lms.course_service.security;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

@Component
public class IdentityExtractor {

    public RequestIdentity extract(HttpServletRequest request){

        String gatewayAuth = request.getHeader("X-Gateway-Auth");
        if (!"true".equals(gatewayAuth)) {
            throw new SecurityException("Request must come through gateway");
        }

        String userId = request.getHeader("X-User-Id");
        String roles = request.getHeader("X-User-Roles");

        if(userId == null || userId.isBlank()){
            throw new SecurityException("Missing X-User-Id");
        }
        return new RequestIdentity(userId, roles);
    }
}
