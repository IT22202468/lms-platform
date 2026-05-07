package com.lms.course_service.dto;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.core.io.ByteArrayResource;
import static org.assertj.core.api.Assertions.assertThat;
import java.time.Instant;
import java.util.List;

class RemainingDtoTest {

    @Test
    void testPageResponse() {
        PageResponse<String> resp = new PageResponse<>(List.of("a"), 1, 10, 100L, 10);
        assertThat(resp.items()).containsExactly("a");
        assertThat(resp.page()).isEqualTo(1);
        assertThat(resp.size()).isEqualTo(10);
        assertThat(resp.totalElements()).isEqualTo(100L);
        assertThat(resp.totalPages()).isEqualTo(10);
    }

    @Test
    void testCourseResponse() {
        Instant now = Instant.now();
        CourseResponse resp = new CourseResponse("c1", "T", "D", "i1", "N", "U", List.of(), true, now, now);
        assertThat(resp.getId()).isEqualTo("c1");
        assertThat(resp.getTitle()).isEqualTo("T");
        assertThat(resp.getDescription()).isEqualTo("D");
        assertThat(resp.getInstructorId()).isEqualTo("i1");
        assertThat(resp.getInstructorName()).isEqualTo("N");
        assertThat(resp.getThumbnailImageUrl()).isEqualTo("U");
        assertThat(resp.getLectureContents()).isEmpty();
        assertThat(resp.isPublished()).isTrue();
        assertThat(resp.getCreatedAt()).isEqualTo(now);
        assertThat(resp.getModifiedAt()).isEqualTo(now);
        
        resp.setId("c2");
        assertThat(resp.getId()).isEqualTo("c2");
    }

    @Test
    void testCourseStudentResponse() {
        Instant now = Instant.now();
        CourseStudentResponse resp = new CourseStudentResponse("s1", now);
        assertThat(resp.getStudentId()).isEqualTo("s1");
        assertThat(resp.getEnrolledAt()).isEqualTo(now);
        
        resp.setStudentId("s2");
        assertThat(resp.getStudentId()).isEqualTo("s2");
    }

    @Test
    void testServedResource() {
        ByteArrayResource res = new ByteArrayResource("data".getBytes());
        ServedResource served = new ServedResource(res, MediaType.APPLICATION_PDF);
        assertThat(served.resource()).isEqualTo(res);
        assertThat(served.mediaType()).isEqualTo(MediaType.APPLICATION_PDF);
    }
}
