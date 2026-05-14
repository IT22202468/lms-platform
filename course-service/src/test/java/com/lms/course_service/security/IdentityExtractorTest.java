package com.lms.course_service.security;

import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IdentityExtractorTest {

    @Mock
    private HttpServletRequest request;

    @Mock
    private JwtService jwtService;

    private IdentityExtractor extractor;

    @BeforeEach
    void setUp() {
        extractor = new IdentityExtractor(jwtService);
    }

    @Test
    void extract_missingAuthorizationHeader_throws() {
        when(request.getHeader("Authorization")).thenReturn(null);

        assertThatThrownBy(() -> extractor.extract(request))
                .isInstanceOf(SecurityException.class)
                .hasMessageContaining("Authorization");
    }

    @Test
    void extract_nonBearerAuthorizationHeader_throws() {
        when(request.getHeader("Authorization")).thenReturn("Basic dXNlcjpwYXNz");

        assertThatThrownBy(() -> extractor.extract(request))
                .isInstanceOf(SecurityException.class)
                .hasMessageContaining("Authorization");
    }

    @Test
    void extract_invalidToken_propagatesException() {
        when(request.getHeader("Authorization")).thenReturn("Bearer bad.token.here");
        when(jwtService.parseToken("bad.token.here")).thenThrow(new RuntimeException("invalid token"));

        assertThatThrownBy(() -> extractor.extract(request))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    void extract_success() {
        when(request.getHeader("Authorization")).thenReturn("Bearer valid.jwt.token");
        Claims claims = mock(Claims.class);
        when(claims.getSubject()).thenReturn("user-1");
        when(claims.get("roles", String.class)).thenReturn("STUDENT,INSTRUCTOR");
        when(claims.get("email", String.class)).thenReturn("user@example.com");
        when(jwtService.parseToken("valid.jwt.token")).thenReturn(claims);

        RequestIdentity id = extractor.extract(request);

        assertThat(id.getUserId()).isEqualTo("user-1");
        assertThat(id.hasRole("STUDENT")).isTrue();
        assertThat(id.hasRole("INSTRUCTOR")).isTrue();
        assertThat(id.getEmail()).isEqualTo("user@example.com");
    }

    @Test
    void extract_missingSubjectInToken_throws() {
        when(request.getHeader("Authorization")).thenReturn("Bearer valid.jwt.token");
        Claims claims = mock(Claims.class);
        when(claims.getSubject()).thenReturn(null);
        when(jwtService.parseToken("valid.jwt.token")).thenReturn(claims);

        assertThatThrownBy(() -> extractor.extract(request))
                .isInstanceOf(SecurityException.class)
                .hasMessageContaining("user ID");
    }
}
