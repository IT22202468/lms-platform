package com.lms.course_service.security;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RequestIdentityTest {

    @Test
    void hasRole_caseInsensitive() {
        RequestIdentity id = new RequestIdentity("u1", "student");

        assertThat(id.hasRole("STUDENT")).isTrue();
        assertThat(id.hasRole("student")).isTrue();
    }

    @Test
    void parsesCommaSeparatedRoles_trimsAndIgnoresBlanks() {
        RequestIdentity id = new RequestIdentity("u1", " ADMIN , , instructor ");

        assertThat(id.hasRole("ADMIN")).isTrue();
        assertThat(id.hasRole("INSTRUCTOR")).isTrue();
        assertThat(id.hasRole("STUDENT")).isFalse();
    }

    @Test
    void nullOrBlankRolesHeader_yieldsNoRoles() {
        RequestIdentity nullRoles = new RequestIdentity("u1", null);
        RequestIdentity blankRoles = new RequestIdentity("u1", "   ");

        assertThat(nullRoles.hasRole("ADMIN")).isFalse();
        assertThat(blankRoles.hasRole("ADMIN")).isFalse();
    }

    @Test
    void getUserId_returnsConstructorValue() {
        RequestIdentity id = new RequestIdentity("abc", "STUDENT");

        assertThat(id.getUserId()).isEqualTo("abc");
    }
}
