package com.lms.course_service.dto;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import java.util.List;

class MoreDtoTest {

    @Test
    void testCreateCourseRequestFull() {
        LectureContentDto content = new LectureContentDto();
        content.setTitle("L1");
        CreateCourseRequest req = new CreateCourseRequest();
        req.setTitle("T");
        req.setDescription("D");
        req.setThumbnailImageUrl("U");
        req.setLectureContents(List.of(content));
        
        assertThat(req.getTitle()).isEqualTo("T");
        assertThat(req.getDescription()).isEqualTo("D");
        assertThat(req.getThumbnailImageUrl()).isEqualTo("U");
        assertThat(req.getLectureContents()).hasSize(1);
    }

    @Test
    void testUpdateCourseRequestFull() {
        UpdateCourseRequest req = new UpdateCourseRequest();
        req.setTitle("T");
        req.setDescription("D");
        req.setThumbnailImageUrl("U");
        req.setLectureContents(List.of());
        
        assertThat(req.getTitle()).isEqualTo("T");
        assertThat(req.getDescription()).isEqualTo("D");
        assertThat(req.getThumbnailImageUrl()).isEqualTo("U");
        assertThat(req.getLectureContents()).isEmpty();
    }
}
