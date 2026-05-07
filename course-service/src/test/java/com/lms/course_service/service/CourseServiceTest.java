package com.lms.course_service.service;

import com.lms.course_service.dto.CreateCourseRequest;
import com.lms.course_service.dto.ServedResource;
import com.lms.course_service.dto.UpdateCourseRequest;
import com.lms.course_service.exception.CourseNotFoundException;
import com.lms.course_service.exception.UnauthorizedException;
import com.lms.course_service.exception.ValidationException;
import com.lms.course_service.model.Course;
import com.lms.course_service.model.Enrollment;
import com.lms.course_service.repo.CourseRepository;
import com.lms.course_service.repo.EnrollmentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CourseServiceTest {

    @Mock
    private CourseRepository courseRepo;

    @Mock
    private EnrollmentRepository enrollmentRepo;

    @Mock
    private CourseFileStorageService fileStorage;

    @InjectMocks
    private CourseService courseService;

    private Course draftCourse;
    private Course publishedCourse;

    @BeforeEach
    void setUp() {
        draftCourse = new Course("Title", "Desc", "inst-1");
        draftCourse.setId("c1");
        draftCourse.setPublished(false);

        publishedCourse = new Course("Pub", "D", "inst-1");
        publishedCourse.setId("c2");
        publishedCourse.setPublished(true);
    }

    @Test
    void createCourse_savesWithInstructorId() {
        CreateCourseRequest req = new CreateCourseRequest();
        req.setTitle("My course title here");
        req.setDescription("My course description long enough");

        Course saved = new Course(req.getTitle(), req.getDescription(), "i99");
        saved.setId("new-id");
        when(courseRepo.save(any(Course.class))).thenAnswer(inv -> {
            Course c = inv.getArgument(0);
            c.setId("new-id");
            return c;
        });

        Course result = courseService.createCourse("i99", "instr@example.com", req);

        assertThat(result.getInstructorId()).isEqualTo("i99");
        assertThat(result.getTitle()).isEqualTo(req.getTitle());
        verify(courseRepo).save(any(Course.class));
    }

    @Test
    void listPublishedCourses_delegatesToRepository() {
        Pageable p = PageRequest.of(0, 10);
        Page<Course> page = new PageImpl<>(List.of(publishedCourse));
        when(courseRepo.findByPublishedTrue(p)).thenReturn(page);

        Page<Course> result = courseService.listPublishedCourses(0, 10);

        assertThat(result.getContent()).containsExactly(publishedCourse);
        verify(courseRepo).findByPublishedTrue(p);
    }

    @Test
    void listInstructorCourses_delegatesToRepository() {
        Pageable p = PageRequest.of(1, 5);
        Page<Course> page = new PageImpl<>(List.of(draftCourse));
        when(courseRepo.findByInstructorId("inst-1", p)).thenReturn(page);

        Page<Course> result = courseService.listInstructorCourses("inst-1", 1, 5);

        assertThat(result.getContent()).containsExactly(draftCourse);
    }

    @Test
    void publishCourse_notFound_throws() {
        when(courseRepo.findById("x")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> courseService.publishCourse("inst-1", "x"))
                .isInstanceOf(CourseNotFoundException.class)
                .hasMessageContaining("x");
    }

    @Test
    void publishCourse_wrongInstructor_throws() {
        when(courseRepo.findById("c1")).thenReturn(Optional.of(draftCourse));

        assertThatThrownBy(() -> courseService.publishCourse("other", "c1"))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("Not your course");
    }

    @Test
    void publishCourse_success_setsPublishedAndSaves() {
        when(courseRepo.findById("c1")).thenReturn(Optional.of(draftCourse));
        when(courseRepo.save(any(Course.class))).thenAnswer(inv -> inv.getArgument(0));

        Course result = courseService.publishCourse("inst-1", "c1");

        assertThat(result.isPublished()).isTrue();
        verify(courseRepo).save(draftCourse);
    }

    @Test
    void getCourseById_notFound_throws() {
        when(courseRepo.findById("x")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> courseService.getCourseById("u1", "x"))
                .isInstanceOf(CourseNotFoundException.class)
                .hasMessageContaining("x");
    }

    @Test
    void getCourseById_unpublished_notInstructor_throws() {
        when(courseRepo.findById("c1")).thenReturn(Optional.of(draftCourse));

        assertThatThrownBy(() -> courseService.getCourseById("student-1", "c1"))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("Forbidden");
    }

    @Test
    void getCourseById_unpublished_instructor_ok() {
        when(courseRepo.findById("c1")).thenReturn(Optional.of(draftCourse));

        Course result = courseService.getCourseById("inst-1", "c1");

        assertThat(result).isSameAs(draftCourse);
    }

    @Test
    void getCourseById_published_anyUser_ok() {
        when(courseRepo.findById("c2")).thenReturn(Optional.of(publishedCourse));

        Course result = courseService.getCourseById("student-1", "c2");

        assertThat(result).isSameAs(publishedCourse);
    }

    @Test
    void updateCourse_wrongInstructor_throws() {
        when(courseRepo.findById("c1")).thenReturn(Optional.of(draftCourse));
        UpdateCourseRequest req = new UpdateCourseRequest();
        req.setTitle("Updated title here");
        req.setDescription("Updated description text long enough");

        assertThatThrownBy(() -> courseService.updateCourse("other", "x@example.com", "c1", req))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void updateCourse_success() {
        when(courseRepo.findById("c1")).thenReturn(Optional.of(draftCourse));
        when(courseRepo.save(any(Course.class))).thenAnswer(inv -> inv.getArgument(0));
        UpdateCourseRequest req = new UpdateCourseRequest();
        req.setTitle("New title goes here");
        req.setDescription("New description goes here ok");

        Course result = courseService.updateCourse("inst-1", "inst@example.com", "c1", req);

        assertThat(result.getTitle()).isEqualTo(req.getTitle());
        assertThat(result.getDescription()).isEqualTo(req.getDescription());
    }

    @Test
    void deleteCourse_deletesEnrollmentsAndCourse() {
        when(courseRepo.findById("c1")).thenReturn(Optional.of(draftCourse));

        courseService.deleteCourse("inst-1", "c1");

        verify(enrollmentRepo).deleteByCourseId("c1");
        verify(courseRepo).delete(draftCourse);
    }

    @Test
    void deleteCourse_wrongInstructor_throws() {
        when(courseRepo.findById("c1")).thenReturn(Optional.of(draftCourse));

        assertThatThrownBy(() -> courseService.deleteCourse("other", "c1"))
                .isInstanceOf(UnauthorizedException.class);
        verify(enrollmentRepo, never()).deleteByCourseId(any());
        verify(courseRepo, never()).delete(any());
    }

    @Test
    void listCourseStudents_wrongInstructor_throws() {
        when(courseRepo.findById("c1")).thenReturn(Optional.of(draftCourse));

        assertThatThrownBy(() -> courseService.listCourseStudents("other", "c1", 0, 10))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void listCourseStudents_success() {
        when(courseRepo.findById("c1")).thenReturn(Optional.of(draftCourse));
        Page<Enrollment> page = new PageImpl<>(List.of(new Enrollment("c1", "s1")));
        when(enrollmentRepo.findByCourseId(eq("c1"), any(Pageable.class))).thenReturn(page);

        Page<Enrollment> result = courseService.listCourseStudents("inst-1", "c1", 0, 10);

        assertThat(result.getContent()).hasSize(1);
    }

    @Test
    void enroll_unpublished_throws() {
        when(courseRepo.findById("c1")).thenReturn(Optional.of(draftCourse));

        assertThatThrownBy(() -> courseService.enroll("s1", "c1"))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("not published");
    }

    @Test
    void enroll_alreadyEnrolled_throws() {
        when(courseRepo.findById("c2")).thenReturn(Optional.of(publishedCourse));
        when(enrollmentRepo.findByCourseIdAndStudentId("c2", "s1"))
                .thenReturn(Optional.of(new Enrollment("c2", "s1")));

        assertThatThrownBy(() -> courseService.enroll("s1", "c2"))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Already enrolled");
        verify(enrollmentRepo, never()).save(any());
    }

    @Test
    void enroll_success_savesEnrollment() {
        when(courseRepo.findById("c2")).thenReturn(Optional.of(publishedCourse));
        when(enrollmentRepo.findByCourseIdAndStudentId("c2", "s1")).thenReturn(Optional.empty());
        when(enrollmentRepo.save(any(Enrollment.class))).thenAnswer(inv -> inv.getArgument(0));

        courseService.enroll("s1", "c2");

        ArgumentCaptor<Enrollment> cap = ArgumentCaptor.forClass(Enrollment.class);
        verify(enrollmentRepo).save(cap.capture());
        assertThat(cap.getValue().getCourseId()).isEqualTo("c2");
        assertThat(cap.getValue().getStudentId()).isEqualTo("s1");
    }

    @Test
    void listStudentEnrollments_delegates() {
        Page<Enrollment> page = new PageImpl<>(List.of());
        when(enrollmentRepo.findByStudentId("s1", PageRequest.of(0, 10))).thenReturn(page);

        Page<Enrollment> result = courseService.listStudentEnrollments("s1", 0, 10);

        assertThat(result).isSameAs(page);
    }

    @Test
    void uploadThumbnail_success() throws IOException {
        when(courseRepo.findById("c1")).thenReturn(Optional.of(draftCourse));
        when(courseRepo.save(any(Course.class))).thenAnswer(inv -> inv.getArgument(0));
        MockMultipartFile file = new MockMultipartFile("file", "test.png", "image/png", "data".getBytes());

        Course result = courseService.uploadThumbnail("inst-1", "Instructor", "c1", file);

        assertThat(result.getThumbnailImageUrl()).isEqualTo("/courses/c1/thumbnail");
        verify(fileStorage).saveThumbnail(eq("c1"), eq(file), anyString());
    }

    @Test
    void uploadThumbnail_fileMissing_throws() {
        assertThatThrownBy(() -> courseService.uploadThumbnail("inst-1", "Instructor", "c1", null))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("File is required");
    }

    @Test
    void uploadThumbnail_unsupportedExtension_throws() {
        when(courseRepo.findById("c1")).thenReturn(Optional.of(draftCourse));
        MockMultipartFile file = new MockMultipartFile("file", "test.txt", "text/plain", "data".getBytes());

        assertThatThrownBy(() -> courseService.uploadThumbnail("inst-1", "Instructor", "c1", file))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Thumbnail must be");
    }

    @Test
    void uploadThumbnail_storageFailure_throws() throws IOException {
        when(courseRepo.findById("c1")).thenReturn(Optional.of(draftCourse));
        MockMultipartFile file = new MockMultipartFile("file", "test.png", "image/png", "data".getBytes());
        doThrow(new IOException("disk full")).when(fileStorage).saveThumbnail(eq("c1"), eq(file), eq("png"));

        assertThatThrownBy(() -> courseService.uploadThumbnail("inst-1", "Instructor", "c1", file))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Failed to store thumbnail");
    }

    @Test
    void appendMaterialFromUpload_success() throws IOException {
        when(courseRepo.findById("c1")).thenReturn(Optional.of(draftCourse));
        when(courseRepo.save(any(Course.class))).thenAnswer(inv -> inv.getArgument(0));
        MockMultipartFile file = new MockMultipartFile("file", "test.pdf", "application/pdf", "data".getBytes());

        Course result = courseService.appendMaterialFromUpload("inst-1", "Instructor", "c1", "Title", "Desc", file);

        assertThat(result.getLectureContents()).hasSize(1);
        assertThat(result.getLectureContents().get(0).getTitle()).isEqualTo("Title");
        verify(fileStorage).saveMaterialFile(eq("c1"), anyString(), eq(file));
    }

    @Test
    void appendMaterialFromUpload_usesFilenameExtensionWhenContentTypeMissing() throws IOException {
        when(courseRepo.findById("c1")).thenReturn(Optional.of(draftCourse));
        when(courseRepo.save(any(Course.class))).thenAnswer(inv -> inv.getArgument(0));
        MockMultipartFile file = new MockMultipartFile("file", "lesson.docx", null, "data".getBytes());

        Course result = courseService.appendMaterialFromUpload("inst-1", "Instructor", "c1", "Title", "Desc", file);

        assertThat(result.getLectureContents()).hasSize(1);
        assertThat(result.getLectureContents().get(0).getContentType())
                .isEqualTo("application/vnd.openxmlformats-officedocument.wordprocessingml.document");
    }

    @Test
    void appendMaterialFromUpload_blankTitle_throws() {
        MockMultipartFile file = new MockMultipartFile("file", "test.pdf", "application/pdf", "data".getBytes());

        assertThatThrownBy(() -> courseService.appendMaterialFromUpload("inst-1", "Instructor", "c1", " ", "Desc", file))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Title is required");
    }

    @Test
    void appendMaterialFromUpload_blankDescription_throws() {
        MockMultipartFile file = new MockMultipartFile("file", "test.pdf", "application/pdf", "data".getBytes());

        assertThatThrownBy(() -> courseService.appendMaterialFromUpload("inst-1", "Instructor", "c1", "Title", " ", file))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Description is required");
    }

    @Test
    void appendMaterialFromUpload_unsupportedType_throws() {
        when(courseRepo.findById("c1")).thenReturn(Optional.of(draftCourse));
        MockMultipartFile file = new MockMultipartFile("file", "archive.zip", "application/zip", "data".getBytes());

        assertThatThrownBy(() -> courseService.appendMaterialFromUpload("inst-1", "Instructor", "c1", "Title", "Desc", file))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Unsupported file type for course material");
    }

    @Test
    void appendMaterialFromUpload_storageFailure_throws() throws IOException {
        when(courseRepo.findById("c1")).thenReturn(Optional.of(draftCourse));
        MockMultipartFile file = new MockMultipartFile("file", "test.pdf", "application/pdf", "data".getBytes());
        doThrow(new IOException("disk full")).when(fileStorage).saveMaterialFile(eq("c1"), anyString(), eq(file));

        assertThatThrownBy(() -> courseService.appendMaterialFromUpload("inst-1", "Instructor", "c1", "Title", "Desc", file))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Failed to store material file");
    }

    @Test
    void loadThumbnailForUser_success() throws IOException {
        draftCourse.setInstructorId("u1");
        when(courseRepo.findById("c1")).thenReturn(Optional.of(draftCourse));
        ServedResource res = new ServedResource(new ByteArrayResource("d".getBytes()), null);
        when(fileStorage.resolveThumbnail("c1")).thenReturn(res);

        ServedResource result = courseService.loadThumbnailForUser("u1", "c1");
        assertThat(result).isSameAs(res);
    }

    @Test
    void loadThumbnailForUser_missingFile_throws() throws IOException {
        draftCourse.setPublished(true);
        when(courseRepo.findById("c1")).thenReturn(Optional.of(draftCourse));
        when(fileStorage.resolveThumbnail("c1")).thenReturn(null);

        assertThatThrownBy(() -> courseService.loadThumbnailForUser("u1", "c1"))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Thumbnail not available");
    }

    @Test
    void loadThumbnailForUser_unpublishedAndNotInstructor_throws() {
        when(courseRepo.findById("c1")).thenReturn(Optional.of(draftCourse));

        assertThatThrownBy(() -> courseService.loadThumbnailForUser("student-1", "c1"))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("Forbidden");
    }

    @Test
    void loadMaterialForUser_success() throws IOException {
        draftCourse.setInstructorId("u1");
        com.lms.course_service.model.LectureContent meta = new com.lms.course_service.model.LectureContent();
        meta.setMaterialId("m1");
        meta.setContentType("application/pdf");
        draftCourse.getLectureContents().add(meta);
        
        when(courseRepo.findById("c1")).thenReturn(Optional.of(draftCourse));
        ServedResource res = new ServedResource(new ByteArrayResource("d".getBytes()), null);
        when(fileStorage.resolveMaterialFile("c1", "m1", "application/pdf")).thenReturn(res);

        CourseService.ServedMaterial result = courseService.loadMaterialForUser("u1", "c1", "m1");
        assertThat(result.resource()).isSameAs(res.resource());
    }

    @Test
    void loadMaterialForUser_missingMetadata_throws() throws IOException {
        when(courseRepo.findById("c1")).thenReturn(Optional.of(publishedCourse));

        assertThatThrownBy(() -> courseService.loadMaterialForUser("student-1", "c1", "missing"))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Material not found");
    }

    @Test
    void loadMaterialForUser_notEnrolled_throws() throws IOException {
        com.lms.course_service.model.LectureContent meta = new com.lms.course_service.model.LectureContent();
        meta.setMaterialId("m1");
        meta.setContentType("application/pdf");
        publishedCourse.getLectureContents().add(meta);
        when(courseRepo.findById("c2")).thenReturn(Optional.of(publishedCourse));
        when(enrollmentRepo.findByCourseIdAndStudentId("c2", "student-1")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> courseService.loadMaterialForUser("student-1", "c2", "m1"))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("Enroll to access course materials");
    }

    @Test
    void loadMaterialForUser_missingFile_throws() throws IOException {
        com.lms.course_service.model.LectureContent meta = new com.lms.course_service.model.LectureContent();
        meta.setMaterialId("m1");
        meta.setContentType("application/pdf");
        draftCourse.setPublished(true);
        draftCourse.getLectureContents().add(meta);
        when(courseRepo.findById("c1")).thenReturn(Optional.of(draftCourse));
        when(enrollmentRepo.findByCourseIdAndStudentId("c1", "student-1")).thenReturn(Optional.of(new Enrollment("c1", "student-1")));
        when(fileStorage.resolveMaterialFile("c1", "m1", "application/pdf")).thenReturn(null);

        assertThatThrownBy(() -> courseService.loadMaterialForUser("student-1", "c1", "m1"))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Material file missing");
    }

    @Test
    void filenameForMaterialDownload_formatsExtensionOnce() {
        com.lms.course_service.model.LectureContent meta = new com.lms.course_service.model.LectureContent();
        meta.setTitle("Intro to Testing");
        meta.setContentType("application/pdf");

        assertThat(courseService.filenameForMaterialDownload(meta)).isEqualTo("Intro_to_Testing.pdf");

        meta.setTitle("slides.pdf");
        assertThat(courseService.filenameForMaterialDownload(meta)).isEqualTo("slides.pdf");
    }

    @Test
    void findMaterialMeta_returnsMatchAndNullWhenMissing() {
        com.lms.course_service.model.LectureContent meta = new com.lms.course_service.model.LectureContent();
        meta.setMaterialId("m1");
        draftCourse.setLectureContents(null);
        assertThat(courseService.findMaterialMeta(draftCourse, "m1")).isNull();

        draftCourse.setLectureContents(List.of(meta));
        assertThat(courseService.findMaterialMeta(draftCourse, "m1")).isSameAs(meta);
        assertThat(courseService.findMaterialMeta(draftCourse, "missing")).isNull();
    }

    @Test
    void deleteCourse_notFound_throws() {
        when(courseRepo.findById("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> courseService.deleteCourse("inst-1", "missing"))
                .isInstanceOf(CourseNotFoundException.class);
    }

    @Test
    void enroll_notFound_throws() {
        when(courseRepo.findById("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> courseService.enroll("s1", "missing"))
                .isInstanceOf(CourseNotFoundException.class);
    }

    @Test
    void listCourseStudents_notFound_throws() {
        when(courseRepo.findById("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> courseService.listCourseStudents("inst-1", "missing", 0, 10))
                .isInstanceOf(CourseNotFoundException.class);
    }
}
