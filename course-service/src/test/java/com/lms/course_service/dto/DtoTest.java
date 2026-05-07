package com.lms.course_service.dto;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import java.util.List;

class DtoTest {

    @Test
    void testCreateCourseRequest() {
        CreateCourseRequest req = new CreateCourseRequest();
        req.setTitle("T");
        req.setDescription("D");
        req.setThumbnailImageUrl("U");
        req.setLectureContents(List.of());
        
        assertThat(req.getTitle()).isEqualTo("T");
        assertThat(req.getDescription()).isEqualTo("D");
        assertThat(req.getThumbnailImageUrl()).isEqualTo("U");
        assertThat(req.getLectureContents()).isEmpty();
    }

    @Test
    void testUpdateCourseRequest() {
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

    @Test
    void testLectureContentDto() {
        LectureContentDto dto = new LectureContentDto();
        dto.setMaterialId("m1");
        dto.setTitle("T");
        dto.setContentType("type");
        dto.setContentUrl("url");
        dto.setDescription("desc");
        dto.setDurationSeconds(10L);
        
        assertThat(dto.getMaterialId()).isEqualTo("m1");
        assertThat(dto.getTitle()).isEqualTo("T");
        assertThat(dto.getContentType()).isEqualTo("type");
        assertThat(dto.getContentUrl()).isEqualTo("url");
        assertThat(dto.getDescription()).isEqualTo("desc");
        assertThat(dto.getDurationSeconds()).isEqualTo(10L);
    }
}
