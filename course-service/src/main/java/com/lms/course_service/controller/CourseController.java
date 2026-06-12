package com.lms.course_service.controller;

import com.lms.course_service.dto.CourseResponse;
import com.lms.course_service.dto.CourseStudentResponse;
import com.lms.course_service.dto.CreateCourseRequest;
import com.lms.course_service.dto.LectureContentDto;
import com.lms.course_service.dto.PageResponse;
import com.lms.course_service.dto.UpdateCourseRequest;
import com.lms.course_service.exception.UnauthorizedException;
import com.lms.course_service.model.Course;
import com.lms.course_service.model.Enrollment;
import com.lms.course_service.security.IdentityExtractor;
import com.lms.course_service.security.RequestIdentity;
import com.lms.course_service.service.CourseService;
import com.lms.course_service.service.CourseService.ServedMaterial;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;

@RestController
public class CourseController {
    public final CourseService courseService;
    private final IdentityExtractor identityExtractor;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private static final Logger log = LoggerFactory.getLogger(CourseController.class);

    public CourseController(CourseService courseService, IdentityExtractor identityExtractor,
                            KafkaTemplate<String, String> kafkaTemplate, ObjectMapper objectMapper) {
        this.courseService = courseService;
        this.identityExtractor = identityExtractor;
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    private static String instructorLabel(RequestIdentity id) {
        return id.getEmail() != null ? id.getEmail() : "";
    }

    @GetMapping("/courses")
    public PageResponse<CourseResponse> listCourses(
            HttpServletRequest request,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size
    ) {
        identityExtractor.extract(request);
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

        Course course = courseService.createCourse(id.getUserId(), instructorLabel(id), req);
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

        return toResponse(courseService.updateCourse(id.getUserId(), instructorLabel(id), courseId, req));
    }

    @PostMapping(path = "/courses/{courseId}/thumbnail", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public CourseResponse uploadThumbnail(
            HttpServletRequest request,
            @PathVariable String courseId,
            @RequestPart("file") MultipartFile file
    ) {
        RequestIdentity id = identityExtractor.extract(request);
        requireRole(id, "INSTRUCTOR");
        Course course = courseService.uploadThumbnail(id.getUserId(), instructorLabel(id), courseId, file);
        log.info("Received file: {}", file.getOriginalFilename());
        return toResponse(course);
    }

    @PostMapping(path = "/courses/{courseId}/materials", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public CourseResponse uploadMaterial(
            HttpServletRequest request,
            @PathVariable String courseId,
            @RequestPart("file") MultipartFile file,
            @RequestParam("title") String title,
            @RequestParam("description") String description
    ) {
        RequestIdentity id = identityExtractor.extract(request);
        requireRole(id, "INSTRUCTOR");
        Course course = courseService.appendMaterialFromUpload(id.getUserId(), instructorLabel(id), courseId, title, description, file);
        log.info("Received file: {}", file.getOriginalFilename());
        return toResponse(course);
    }

    @GetMapping("/courses/{courseId}/thumbnail")
    public ResponseEntity<Resource> streamThumbnail(HttpServletRequest request, @PathVariable String courseId) throws IOException {
        RequestIdentity id = identityExtractor.extract(request);
        var served = courseService.loadThumbnailForUser(id.getUserId(), courseId);
        return ResponseEntity.ok()
                .contentType(served.mediaType())
                .header(HttpHeaders.CACHE_CONTROL, "private, max-age=3600")
                .body(served.resource());
    }

    @GetMapping("/courses/{courseId}/materials/{materialId}/download")
    public ResponseEntity<Resource> downloadMaterial(
            HttpServletRequest request,
            @PathVariable String courseId,
            @PathVariable String materialId
    ) throws IOException {
        RequestIdentity id = identityExtractor.extract(request);
        ServedMaterial sm = courseService.loadMaterialForUser(id.getUserId(), courseId, materialId);
        ContentDisposition disposition = ContentDisposition.attachment().filename(sm.filename()).build();
        return ResponseEntity.ok()
                .contentType(sm.mediaType())
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .body(sm.resource());
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

        // publish enrollment event (best-effort)
        try {
            Map<String, Object> ev = Map.of(
                    "courseId", courseId,
                    "studentId", id.getUserId(),
                    "studentEmail", id.getEmail(),
                    "enrolledAt", Instant.now().toString()
            );
            kafkaTemplate.send("course.enrolled", objectMapper.writeValueAsString(ev));
        } catch (JsonProcessingException e) {
            log.warn("Failed to publish course.enrolled event", e);
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("message", "Enrolled"));
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
            throw new UnauthorizedException("Forbidden: requires " + role + " role");
        }
    }

    private CourseResponse toResponse(Course c) {
        List<LectureContentDto> lectureContents = c.getLectureContents() == null
                ? List.of()
                : c.getLectureContents().stream()
                .map(item -> {
                    LectureContentDto dto = new LectureContentDto();
                    dto.setMaterialId(item.getMaterialId());
                    dto.setTitle(item.getTitle());
                    dto.setContentType(item.getContentType());
                    dto.setContentUrl(item.getContentUrl());
                    dto.setDescription(item.getDescription());
                    dto.setDurationSeconds(item.getDurationSeconds());
                    dto.setUploadedAt(item.getUploadedAt());
                    return dto;
                })
                .toList();

        return new CourseResponse(
                c.getId(),
                c.getTitle(),
                c.getDescription(),
                c.getInstructorId(),
                c.getInstructorName(),
                c.getThumbnailImageUrl(),
                lectureContents,
                c.isPublished(),
                c.getCreatedAt(),
                c.getModifiedAt()
        );
    }
}
