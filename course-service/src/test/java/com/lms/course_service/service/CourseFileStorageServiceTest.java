package com.lms.course_service.service;

import com.lms.course_service.dto.ServedResource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CourseFileStorageServiceTest {

    private CourseFileStorageService storageService;
    private Path tempDir;

    @BeforeEach
    void setUp(@TempDir Path tempDir) {
        this.tempDir = tempDir;
        storageService = new CourseFileStorageService(tempDir.toString());
    }

    @Test
    void saveThumbnail_createsDirAndFile() throws IOException {
        MockMultipartFile file = new MockMultipartFile("file", "thumb.png", "image/png", "content".getBytes());
        
        storageService.saveThumbnail("c1", file, "png");
        
        Path expectedFile = tempDir.resolve("courses/c1/thumbnail.png");
        assertThat(Files.exists(expectedFile)).isTrue();
        assertThat(Files.readAllBytes(expectedFile)).isEqualTo("content".getBytes());
    }

    @Test
    void saveThumbnail_removesOldThumbnails() throws IOException {
        Path courseDir = tempDir.resolve("courses/c1");
        Files.createDirectories(courseDir);
        Files.write(courseDir.resolve("thumbnail.jpg"), "old".getBytes());
        
        MockMultipartFile file = new MockMultipartFile("file", "new.png", "image/png", "new".getBytes());
        storageService.saveThumbnail("c1", file, "png");
        
        assertThat(Files.exists(courseDir.resolve("thumbnail.jpg"))).isFalse();
        assertThat(Files.exists(courseDir.resolve("thumbnail.png"))).isTrue();
    }

    @Test
    void resolveThumbnail_returnsNullIfMissing() throws IOException {
        assertThat(storageService.resolveThumbnail("missing")).isNull();
    }

    @Test
    void resolveThumbnail_success() throws IOException {
        MockMultipartFile file = new MockMultipartFile("file", "thumb.png", "image/png", "content".getBytes());
        storageService.saveThumbnail("c1", file, "png");
        
        ServedResource res = storageService.resolveThumbnail("c1");
        
        assertThat(res).isNotNull();
        assertThat(res.resource().exists()).isTrue();
    }

    @Test
    void saveAndResolveMaterial_success() throws IOException {
        MockMultipartFile file = new MockMultipartFile("file", "doc.pdf", "application/pdf", "pdf content".getBytes());
        
        storageService.saveMaterialFile("c1", "m1", file);
        ServedResource res = storageService.resolveMaterialFile("c1", "m1", "application/pdf");
        
        assertThat(res).isNotNull();
        assertThat(res.resource().exists()).isTrue();
        assertThat(res.mediaType().toString()).isEqualTo("application/pdf");
    }

    @Test
    void deleteCourseFiles_removesDirectory() throws IOException {
        MockMultipartFile file = new MockMultipartFile("file", "thumb.png", "image/png", "content".getBytes());
        storageService.saveThumbnail("c1", file, "png");
        
        storageService.deleteCourseFiles("c1");
        
        assertThat(Files.exists(tempDir.resolve("courses/c1"))).isFalse();
    }

    @Test
    void thumbnailExtension_validation() {
        assertThat(CourseFileStorageService.thumbnailExtension("image.png")).isEqualTo("png");
        assertThat(CourseFileStorageService.thumbnailExtension("IMAGE.JPG")).isEqualTo("jpg");
        
        assertThatThrownBy(() -> CourseFileStorageService.thumbnailExtension("doc.pdf"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> CourseFileStorageService.thumbnailExtension("noext"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void safeSegment_validation() {
        assertThat(CourseFileStorageService.safeSegment("valid-123")).isEqualTo("valid-123");
        
        assertThatThrownBy(() -> CourseFileStorageService.safeSegment("../evil"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> CourseFileStorageService.safeSegment("a/b"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> CourseFileStorageService.safeSegment(""))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
