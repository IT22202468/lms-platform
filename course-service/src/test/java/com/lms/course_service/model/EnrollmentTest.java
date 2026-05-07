package com.lms.course_service.model;

import org.junit.jupiter.api.Test;
import java.time.Instant;
import static org.assertj.core.api.Assertions.assertThat;

class EnrollmentTest {

    @Test
    void testGettersAndSetters() {
        Enrollment enrollment = new Enrollment();
        Instant now = Instant.now();
        
        enrollment.setId("id1");
        enrollment.setCourseId("c1");
        enrollment.setStudentId("s1");
        enrollment.setEnrolledAt(now);

        assertThat(enrollment.getId()).isEqualTo("id1");
        assertThat(enrollment.getCourseId()).isEqualTo("c1");
        assertThat(enrollment.getStudentId()).isEqualTo("s1");
        assertThat(enrollment.getEnrolledAt()).isEqualTo(now);
    }

    @Test
    void testConstructor() {
        Enrollment enrollment = new Enrollment("c1", "s1");
        assertThat(enrollment.getCourseId()).isEqualTo("c1");
        assertThat(enrollment.getStudentId()).isEqualTo("s1");
        assertThat(enrollment.getEnrolledAt()).isNotNull();
    }
}
