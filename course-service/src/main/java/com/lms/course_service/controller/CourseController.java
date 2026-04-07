package com.lms.course_service.controller;

import com.lms.course_service.dto.CourseResponse;
import com.lms.course_service.dto.CourseStudentResponse;
import com.lms.course_service.dto.CreateCourseRequest;
import com.lms.course_service.dto.UpdateCourseRequest;
import com.lms.course_service.model.Course;
import com.lms.course_service.model.Enrollment;
import com.lms.course_service.security.IdentityExtractor;
import com.lms.course_service.security.RequestIdentity;
import com.lms.course_service.service.CourseService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
public class CourseController {
    public final CourseService courseService;
    private final IdentityExtractor identityExtractor;

    public CourseController(CourseService courseService, IdentityExtractor identityExtractor) {
        this.courseService = courseService;
        this.identityExtractor = identityExtractor;
    }

    @GetMapping("/courses")
    public List<CourseResponse> listCourses() {
        return courseService.listPublishedCourses().stream().map(this::toResponse).toList();
    }

    @GetMapping("/courses/{courseId}")
    public CourseResponse getCourseById(HttpServletRequest request, @PathVariable String courseId) {
        RequestIdentity id = identityExtractor.extract(request);
        return toResponse(courseService.getCourseById(id.getUserId(), courseId));
    }

    @PostMapping("/courses")
    public ResponseEntity<CourseResponse> createCourse(
            HttpServletRequest request,
            @Valid @RequestBody CreateCourseRequest req
    ) {
        RequestIdentity id = identityExtractor.extract(request);
        requireRole(id, "INSTRUCTOR");

        Course course = courseService.createCourse(id.getUserId(), req);
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(course));
    }

    @PutMapping("/courses/{courseId}")
    public CourseResponse updateCourse(
            HttpServletRequest request,
            @PathVariable String courseId,
            @Valid @RequestBody UpdateCourseRequest req
    ) {
        RequestIdentity id = identityExtractor.extract(request);
        requireRole(id, "INSTRUCTOR");

        return toResponse(courseService.updateCourse(id.getUserId(), courseId, req));
    }

    @DeleteMapping("/courses/{courseId}")
    public ResponseEntity<Map<String, String>> deleteCourse(HttpServletRequest request, @PathVariable String courseId) {
        RequestIdentity id = identityExtractor.extract(request);
        requireRole(id, "INSTRUCTOR");

        courseService.deleteCourse(id.getUserId(), courseId);
        return ResponseEntity.ok(Map.of("message", "Course deleted"));
    }

    // Instructor: list my courses
    @GetMapping("/instructor/courses")
    public List<CourseResponse> myCourses(HttpServletRequest request) {
        RequestIdentity id = identityExtractor.extract(request);
        requireRole(id, "INSTRUCTOR");

        return courseService.listInstructorCourses(id.getUserId()).stream().map(this::toResponse).toList();
    }

    // Instructor: publish course
    @PutMapping("/courses/{courseId}/publish")
    public CourseResponse publish(HttpServletRequest request, @PathVariable String courseId) {
        RequestIdentity id = identityExtractor.extract(request);
        requireRole(id, "INSTRUCTOR");

        return toResponse(courseService.publishCourse(id.getUserId(), courseId));
    }

    // Student: enroll
    @PostMapping("/courses/{courseId}/enroll")
    public ResponseEntity<?> enroll(HttpServletRequest request, @PathVariable String courseId) {
        RequestIdentity id = identityExtractor.extract(request);
        requireRole(id, "STUDENT");

        courseService.enroll(id.getUserId(), courseId);
        return ResponseEntity.status(HttpStatus.CREATED).body(java.util.Map.of("message", "Enrolled"));
    }

    // Student: my enrollments
    @GetMapping("/student/enrollments")
    public List<Enrollment> myEnrollments(HttpServletRequest request) {
        RequestIdentity id = identityExtractor.extract(request);
        requireRole(id, "STUDENT");

        return courseService.listStudentEnrollments(id.getUserId());
    }

    @GetMapping("/courses/{courseId}/students")
    public List<CourseStudentResponse> listCourseStudents(HttpServletRequest request, @PathVariable String courseId) {
        RequestIdentity id = identityExtractor.extract(request);
        requireRole(id, "INSTRUCTOR");

        return courseService.listCourseStudents(id.getUserId(), courseId).stream()
                .map(enrollment -> new CourseStudentResponse(enrollment.getStudentId(), enrollment.getEnrolledAt()))
                .toList();
    }

    private void requireRole(RequestIdentity id, String role) {
        if (!id.hasRole(role)) {
            throw new SecurityException("Forbidden");
        }
    }

    private CourseResponse toResponse(Course c) {
        return new CourseResponse(c.getId(), c.getTitle(), c.getDescription(), c.getInstructorId(), c.isPublished(), c.getCreatedAt());
    }
}
