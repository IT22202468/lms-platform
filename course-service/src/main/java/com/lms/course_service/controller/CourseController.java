package com.lms.course_service.controller;

import com.lms.course_service.dto.CourseResponse;
import com.lms.course_service.dto.CourseStudentResponse;
import com.lms.course_service.dto.CreateCourseRequest;
import com.lms.course_service.dto.PageResponse;
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
    public PageResponse<CourseResponse> listCourses(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size
    ) {
        var result = courseService.listPublishedCourses(page, size).map(this::toResponse);
        return new PageResponse<>(
                result.getContent(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages()
        );
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
    public PageResponse<CourseResponse> myCourses(
            HttpServletRequest request,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size
    ) {
        RequestIdentity id = identityExtractor.extract(request);
        requireRole(id, "INSTRUCTOR");

        var result = courseService.listInstructorCourses(id.getUserId(), page, size).map(this::toResponse);
        return new PageResponse<>(
                result.getContent(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages()
        );
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
    public PageResponse<Enrollment> myEnrollments(
            HttpServletRequest request,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size
    ) {
        RequestIdentity id = identityExtractor.extract(request);
        requireRole(id, "STUDENT");

        var result = courseService.listStudentEnrollments(id.getUserId(), page, size);
        return new PageResponse<>(
                result.getContent(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages()
        );
    }

    @GetMapping("/courses/{courseId}/students")
    public PageResponse<CourseStudentResponse> listCourseStudents(
            HttpServletRequest request,
            @PathVariable String courseId,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size
    ) {
        RequestIdentity id = identityExtractor.extract(request);
        requireRole(id, "INSTRUCTOR");

        var result = courseService.listCourseStudents(id.getUserId(), courseId, page, size)
                .map(enrollment -> new CourseStudentResponse(enrollment.getStudentId(), enrollment.getEnrolledAt()));

        return new PageResponse<>(
                result.getContent(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages()
        );
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
