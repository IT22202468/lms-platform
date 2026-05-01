package com.lms.course_service.service;

import com.lms.course_service.dto.CreateCourseRequest;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
}
