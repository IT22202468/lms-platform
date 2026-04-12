package com.lms.api_gateway.config;

import org.springframework.http.HttpHeaders;
import com.lms.api_gateway.security.JwtService;
import io.jsonwebtoken.Claims;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.function.HandlerFilterFunction;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerRequest;
import org.springframework.web.servlet.function.ServerResponse;

import static org.springframework.cloud.gateway.server.mvc.handler.HandlerFunctions.http;
import static org.springframework.cloud.gateway.server.mvc.filter.BeforeFilterFunctions.uri;
import static org.springframework.cloud.gateway.server.mvc.handler.GatewayRouterFunctions.route;

@Configuration
public class GatewayRoutesConfig {

    private static final Logger log = LoggerFactory.getLogger(GatewayRoutesConfig.class);

    private final JwtService jwtService;

    public GatewayRoutesConfig(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Bean
    public RouterFunction<ServerResponse> gatewayRoutes() {
//        return route()
//                .route(req -> req.path().startsWith("/auth/"), http())
//                .before(uri("http://localhost:8081"))
//
//
//                .route(req ->
//                                req.path().startsWith("/courses/")
//                                        || req.path().startsWith("/instructor/")
//                                        || req.path().startsWith("/student/"),
//                        http())
//                .filter(jwtHeaderForwardingFilter())
//                .before(uri("http://localhost:8082"))
//                .build();
//        RouterFunction<ServerResponse> authRoutes =
//                route()
//                        .route(req -> req.path().startsWith("/auth/"), http())
//                        .before(uri("http://localhost:8081"))
//                        .build();
//
//        RouterFunction<ServerResponse> protectedRoutes =
//                route()
//                        .route(req ->
//                                        req.path().startsWith("/courses/")
//                                                || req.path().startsWith("/instructor/")
//                                                || req.path().startsWith("/student/"),
//                                http())
//                        .filter(jwtHeaderForwardingFilter())
//                        .before(uri("http://localhost:8082"))
//                        .build();
//
//        return authRoutes.andOther(protectedRoutes);
        RouterFunction<ServerResponse> authRoutes =
                (RouterFunction<ServerResponse>) route()
                        .route(req -> req.path().startsWith("/auth/"), http())
                        .before(uri("http://localhost:8081"))
                        .build();

        RouterFunction<ServerResponse> protectedRoutes =
                (RouterFunction<ServerResponse>) route()
                        .route(req ->
                                        req.path().startsWith("/courses")
                                                || req.path().startsWith("/instructor")
                                                || req.path().startsWith("/student"),
                                http())
                        .filter(jwtHeaderForwardingFilter())
                        .before(uri("http://localhost:8082"))
                        .build();

        return (RouterFunction<ServerResponse>) authRoutes.andOther(protectedRoutes);
    }

    private HandlerFilterFunction<ServerResponse, ServerResponse> jwtHeaderForwardingFilter() {
        return (request, next) -> {
            String authHeader = request.headers().firstHeader(HttpHeaders.AUTHORIZATION);

            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                log.atWarn()
                        .addKeyValue("event.action", "gateway_jwt_rejected")
                        .addKeyValue("error.category", "missing_bearer")
                        .addKeyValue("http.response.status_code", 401)
                        .addKeyValue("url.path", request.path())
                        .log("Missing or invalid Authorization header");
                return ServerResponse.status(401)
                        .body("{\"message\":\"Missing or invalid Authorization header\"}");
            }

            String token = authHeader.substring(7);

            try {
                Claims claims = jwtService.parseToken(token);

                String userId = claims.getSubject();
                String email = claims.get("email", String.class);
                String roles = claims.get("roles", String.class);

                ServerRequest modified = ServerRequest.from(request)
                        .headers(headers -> {
                            headers.remove("X-User-Id");
                            headers.remove("X-User-Email");
                            headers.remove("X-User-Roles");
                            headers.remove("X-Gateway-Auth");

                            headers.add("X-User-Id", userId);
                            headers.add("X-User-Email", email);
                            headers.add("X-User-Roles", roles);
                            headers.add("X-Gateway-Auth", "true");
                        })
                        .build();

                return next.handle(modified);
            } catch (Exception ex) {
                log.atWarn()
                        .addKeyValue("event.action", "gateway_jwt_rejected")
                        .addKeyValue("error.category", "invalid_token")
                        .addKeyValue("error.type", ex.getClass().getName())
                        .addKeyValue("http.response.status_code", 401)
                        .addKeyValue("url.path", request.path())
                        .log("Invalid or expired token");
                return ServerResponse.status(401)
                        .body("{\"message\":\"Invalid or expired token\"}");
            }
        };
    }


}
