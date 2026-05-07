package com.lms.course_service.model;

import org.junit.jupiter.api.Test;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;

class CourseTest {

    @Test
    void testGettersAndSetters() {
        Course course = new Course();
        Instant now = Instant.now();
        List<LectureContent> contents = new ArrayList<>();
        
        course.setId("id1");
        course.setTitle("Title");
        course.setDescription("Desc");
        course.setInstructorId("inst1");
        course.setInstructorName("Name");
        course.setThumbnailImageUrl("url");
        course.setLectureContents(contents);
        course.setPublished(true);
        course.setCreatedAt(now);
        course.setModifiedAt(now);
        course.setUpdatedAt(now);

        assertThat(course.getId()).isEqualTo("id1");
        assertThat(course.getTitle()).isEqualTo("Title");
        assertThat(course.getDescription()).isEqualTo("Desc");
        assertThat(course.getInstructorId()).isEqualTo("inst1");
        assertThat(course.getInstructorName()).isEqualTo("Name");
        assertThat(course.getThumbnailImageUrl()).isEqualTo("url");
        assertThat(course.getLectureContents()).isSameAs(contents);
        assertThat(course.isPublished()).isTrue();
        assertThat(course.getCreatedAt()).isEqualTo(now);
        assertThat(course.getModifiedAt()).isEqualTo(now);
        assertThat(course.getUpdatedAt()).isEqualTo(now);
    }

    @Test
    void testConstructor() {
        Course course = new Course("Title", "Desc", "inst1");
        assertThat(course.getTitle()).isEqualTo("Title");
        assertThat(course.getDescription()).isEqualTo("Desc");
        assertThat(course.getInstructorId()).isEqualTo("inst1");
        assertThat(course.getCreatedAt()).isNotNull();
    }
}
