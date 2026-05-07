package com.lms.course_service.model;

import org.junit.jupiter.api.Test;
import java.time.Instant;
import static org.assertj.core.api.Assertions.assertThat;

class LectureContentTest {

    @Test
    void testGettersAndSetters() {
        LectureContent content = new LectureContent();
        Instant now = Instant.now();
        
        content.setMaterialId("m1");
        content.setTitle("Title");
        content.setContentType("type");
        content.setContentUrl("url");
        content.setDescription("Desc");
        content.setDurationSeconds(100L);
        content.setUploadedAt(now);

        assertThat(content.getMaterialId()).isEqualTo("m1");
        assertThat(content.getTitle()).isEqualTo("Title");
        assertThat(content.getContentType()).isEqualTo("type");
        assertThat(content.getContentUrl()).isEqualTo("url");
        assertThat(content.getDescription()).isEqualTo("Desc");
        assertThat(content.getDurationSeconds()).isEqualTo(100L);
        assertThat(content.getUploadedAt()).isEqualTo(now);
    }
}
