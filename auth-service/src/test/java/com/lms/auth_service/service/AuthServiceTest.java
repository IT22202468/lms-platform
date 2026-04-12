package com.lms.auth_service.service;

import com.lms.auth_service.dto.LoginRequest;
import com.lms.auth_service.dto.RegisterRequest;
import com.lms.auth_service.exception.UnauthorizedException;
import com.lms.auth_service.exception.ValidationException;
import com.lms.auth_service.model.User;
import com.lms.auth_service.repo.UserRepository;
import com.lms.auth_service.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthService authService;

    private RegisterRequest registerRequest;
    private LoginRequest loginRequest;

    @BeforeEach
    void setUp() {
        registerRequest = new RegisterRequest();
        registerRequest.setEmail("User@Example.com");
        registerRequest.setPassword("password123");

        loginRequest = new LoginRequest();
        loginRequest.setEmail("user@example.com");
        loginRequest.setPassword("password123");
    }

    @Test
    void register_duplicateEmail_throws() {
        when(userRepository.existsByEmail("user@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(registerRequest))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Email already exists");

        verify(userRepository, never()).save(any());
    }

    @Test
    void register_invalidRole_throws() {
        when(userRepository.existsByEmail("user@example.com")).thenReturn(false);
        registerRequest.setRole("SUPERUSER");

        assertThatThrownBy(() -> authService.register(registerRequest))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Invalid role");
    }

    @Test
    void register_blankRole_defaultsToStudent() {
        when(userRepository.existsByEmail("user@example.com")).thenReturn(false);
        registerRequest.setRole("   ");
        when(passwordEncoder.encode("password123")).thenReturn("hash");
        User saved = new User("user@example.com", "hash", Set.of("STUDENT"));
        saved.setId("id-1");
        when(userRepository.save(any(User.class))).thenReturn(saved);
        when(jwtService.generateAccessToken("id-1", "user@example.com", "STUDENT")).thenReturn("jwt-token");

        String token = authService.register(registerRequest);

        assertThat(token).isEqualTo("jwt-token");
        ArgumentCaptor<User> cap = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(cap.capture());
        assertThat(cap.getValue().getRoles()).containsExactly("STUDENT");
    }

    @Test
    void register_saveDuplicateKey_throws() {
        when(userRepository.existsByEmail("user@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("hash");
        when(userRepository.save(any(User.class))).thenThrow(new DuplicateKeyException("dup"));

        assertThatThrownBy(() -> authService.register(registerRequest))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Email already exists");
    }

    @Test
    void register_success_encodesSavesReturnsToken() {
        when(userRepository.existsByEmail("user@example.com")).thenReturn(false);
        registerRequest.setRole("instructor");
        when(passwordEncoder.encode("password123")).thenReturn("encoded-hash");
        User saved = new User("user@example.com", "encoded-hash", Set.of("INSTRUCTOR"));
        saved.setId("user-id-99");
        when(userRepository.save(any(User.class))).thenReturn(saved);
        when(jwtService.generateAccessToken("user-id-99", "user@example.com", "INSTRUCTOR"))
                .thenReturn("access-jwt");

        String token = authService.register(registerRequest);

        assertThat(token).isEqualTo("access-jwt");
        verify(passwordEncoder).encode("password123");
        verify(jwtService).generateAccessToken(eq("user-id-99"), eq("user@example.com"), eq("INSTRUCTOR"));
    }

    @Test
    void login_userNotFound_throws() {
        when(userRepository.findByEmail("user@example.com")).thenReturn(java.util.Optional.empty());

        assertThatThrownBy(() -> authService.login(loginRequest))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("Invalid credentials");
    }

    @Test
    void login_badPassword_throws() {
        User user = new User("user@example.com", "stored-hash", Set.of("STUDENT"));
        user.setId("u1");
        when(userRepository.findByEmail("user@example.com")).thenReturn(java.util.Optional.of(user));
        when(passwordEncoder.matches("password123", "stored-hash")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(loginRequest))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("Invalid credentials");
        verify(jwtService, never()).generateAccessToken(any(), any(), any());
    }

    @Test
    void login_success_returnsToken() {
        User user = new User("user@example.com", "stored-hash", Set.of("STUDENT"));
        user.setId("u1");
        when(userRepository.findByEmail("user@example.com")).thenReturn(java.util.Optional.of(user));
        when(passwordEncoder.matches("password123", "stored-hash")).thenReturn(true);
        when(jwtService.generateAccessToken("u1", "user@example.com", "STUDENT"))
                .thenReturn("login-jwt");

        String token = authService.login(loginRequest);

        assertThat(token).isEqualTo("login-jwt");
    }
}
