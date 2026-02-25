//Service = "Business logic of your system"

package com.lms.auth_service.service;

import com.lms.auth_service.dto.LoginRequest;
import com.lms.auth_service.dto.RegisterRequest;
import com.lms.auth_service.model.User;
import com.lms.auth_service.repo.UserRepository;
import com.lms.auth_service.security.JwtService;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.Set;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public String register(RegisterRequest req){
        String email = req.getEmail().toLowerCase(Locale.ROOT);

        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Email already exists");
        }

        Set<String> allowedRoles = Set.of("STUDENT", "INSTRUCTOR", "ADMIN");

        String role = (req.getRole() == null || req.getRole().isBlank())
                ? "STUDENT"
                : req.getRole().trim().toUpperCase(Locale.ROOT);

        if (!allowedRoles.contains(role)) {
            throw new IllegalArgumentException("Invalid role: must be one of STUDENT, INSTRUCTOR, ADMIN");
        }

        String hash = passwordEncoder.encode(req.getPassword());
        User user = new User(email, hash, Set.of(role));

        try {
            user = userRepository.save(user);
        } catch (DuplicateKeyException e) {
            throw new IllegalArgumentException("Email already exists");
        }

        String rolesCsv = String.join(",", user.getRoles());
        return jwtService.generateAccessToken(user.getId(), user.getEmail(), rolesCsv);

    }

    public String login(LoginRequest req){
        String email = req.getEmail().toLowerCase(Locale.ROOT);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Invalid credentials"));

        if(!passwordEncoder.matches(req.getPassword(), user.getPasswordHash())){
            throw new IllegalArgumentException("Invalid credentials");
        }

        String rolesCsv = String.join(",", user.getRoles());

        return jwtService.generateAccessToken(user.getId(), user.getEmail(), rolesCsv);
    }
}
