package com.lms.course_service.security;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IdentityExtractorTest {

    @Mock
    private HttpServletRequest request;

    private IdentityExtractor extractor;

    @BeforeEach
    void setUp() {
        extractor = new IdentityExtractor();
    }

    @Test
    void extract_missingGatewayHeader_throws() {
        when(request.getHeader("X-Gateway-Auth")).thenReturn(null);

        assertThatThrownBy(() -> extractor.extract(request))
                .isInstanceOf(SecurityException.class)
                .hasMessageContaining("gateway");
    }

    @Test
    void extract_wrongGatewayHeader_throws() {
        when(request.getHeader("X-Gateway-Auth")).thenReturn("false");

        assertThatThrownBy(() -> extractor.extract(request))
                .isInstanceOf(SecurityException.class);
    }

    @Test
    void extract_missingUserId_throws() {
        when(request.getHeader("X-Gateway-Auth")).thenReturn("true");
        when(request.getHeader("X-User-Id")).thenReturn(null);

        assertThatThrownBy(() -> extractor.extract(request))
                .isInstanceOf(SecurityException.class)
                .hasMessageContaining("X-User-Id");
    }

    @Test
    void extract_blankUserId_throws() {
        when(request.getHeader("X-Gateway-Auth")).thenReturn("true");
        when(request.getHeader("X-User-Id")).thenReturn("   ");

        assertThatThrownBy(() -> extractor.extract(request))
                .isInstanceOf(SecurityException.class);
    }

    @Test
    void extract_success() {
        when(request.getHeader("X-Gateway-Auth")).thenReturn("true");
        when(request.getHeader("X-User-Id")).thenReturn("user-1");
        when(request.getHeader("X-User-Roles")).thenReturn("STUDENT,INSTRUCTOR");

        RequestIdentity id = extractor.extract(request);

        assertThat(id.getUserId()).isEqualTo("user-1");
        assertThat(id.hasRole("STUDENT")).isTrue();
        assertThat(id.hasRole("INSTRUCTOR")).isTrue();
    }
}
