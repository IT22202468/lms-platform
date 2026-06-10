//Service = "Business logic of your system"

package com.lms.auth_service.service;

import com.lms.auth_service.dto.LoginRequest;
import com.lms.auth_service.dto.RegisterRequest;
import com.lms.auth_service.exception.UnauthorizedException;
import com.lms.auth_service.exception.ValidationException;
import com.lms.auth_service.model.User;
import com.lms.auth_service.repo.UserRepository;
import com.lms.auth_service.security.JwtService;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Locale;
import java.util.Set;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public AuthService(
        UserRepository userRepository, 
        PasswordEncoder passwordEncoder, 
        JwtService jwtService,
        KafkaTemplate<String, String> kafkaTemplate,
        ObjectMapper objectMapper
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    public String register(RegisterRequest req){
        String email = req.getEmail().toLowerCase(Locale.ROOT);

        if (userRepository.existsByEmail(email)) {
            log.atWarn()
                    .addKeyValue("event.action", "user_register")
                    .addKeyValue("event.outcome", "failure")
                    .addKeyValue("error.category", "duplicate_email")
                    .addKeyValue("user.email", email)
                    .log("Registration failed: email already exists");
            throw new ValidationException("Email already exists");
        }

        Set<String> allowedRoles = Set.of("STUDENT", "INSTRUCTOR", "ADMIN");

        String role = (req.getRole() == null || req.getRole().isBlank())
                ? "STUDENT"
                : req.getRole().trim().toUpperCase(Locale.ROOT);

        if (!allowedRoles.contains(role)) {
            log.atWarn()
                    .addKeyValue("event.action", "user_register")
                    .addKeyValue("event.outcome", "failure")
                    .addKeyValue("error.category", "invalid_role")
                    .addKeyValue("user.email", email)
                    .addKeyValue("user.role", role)
                    .log("Registration failed: invalid role");
            throw new ValidationException("Invalid role: must be one of STUDENT, INSTRUCTOR, ADMIN");
        }

        String hash = passwordEncoder.encode(req.getPassword());
        User user = new User(email, hash, Set.of(role));

        try {
            user = userRepository.save(user);
        } catch (DuplicateKeyException e) {
            log.atWarn()
                    .addKeyValue("event.action", "user_register")
                    .addKeyValue("event.outcome", "failure")
                    .addKeyValue("error.category", "duplicate_email")
                    .addKeyValue("user.email", email)
                    .log("Registration failed: duplicate key on save");
            throw new ValidationException("Email already exists");
        }

        log.atInfo()
                .addKeyValue("event.action", "user_register")
                .addKeyValue("event.outcome", "success")
                .addKeyValue("user.id", user.getId())
                .addKeyValue("user.email", email)
                .addKeyValue("user.role", role)
                .log("User registered successfully");

        publishUserRegisteredEvent(user);

        String rolesCsv = String.join(",", user.getRoles());
        return jwtService.generateAccessToken(user.getId(), user.getEmail(), rolesCsv);

    }

    private void publishUserRegisteredEvent(User user) {
        UserRegisteredEvent event = new UserRegisteredEvent(user.getId(), user.getEmail(), Instant.now());

        try {
            kafkaTemplate.send("user.registered", objectMapper.writeValueAsString(event));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize user registered event", e);
        }
    }

    public String login(LoginRequest req){
        String email = req.getEmail().toLowerCase(Locale.ROOT);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    log.atWarn()
                            .addKeyValue("event.action", "user_login")
                            .addKeyValue("event.outcome", "failure")
                            .addKeyValue("error.category", "user_not_found")
                            .addKeyValue("user.email", email)
                            .log("Login failed: user not found");
                    return new UnauthorizedException("Invalid credentials");
                });

        if(!passwordEncoder.matches(req.getPassword(), user.getPasswordHash())){
            log.atWarn()
                    .addKeyValue("event.action", "user_login")
                    .addKeyValue("event.outcome", "failure")
                    .addKeyValue("error.category", "bad_password")
                    .addKeyValue("user.id", user.getId())
                    .addKeyValue("user.email", email)
                    .log("Login failed: incorrect password");
            throw new UnauthorizedException("Invalid credentials");
        }

        log.atInfo()
                .addKeyValue("event.action", "user_login")
                .addKeyValue("event.outcome", "success")
                .addKeyValue("user.id", user.getId())
                .addKeyValue("user.email", email)
                .log("User logged in successfully");

        String rolesCsv = String.join(",", user.getRoles());

        return jwtService.generateAccessToken(user.getId(), user.getEmail(), rolesCsv);
    }

    record UserRegisteredEvent(String userId, String email, Instant createdAt) {}
}
