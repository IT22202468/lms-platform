package com.lms.course_service.service;

import com.lms.course_service.dto.CreateCourseRequest;
import com.lms.course_service.dto.LectureContentDto;
import com.lms.course_service.dto.ServedResource;
import com.lms.course_service.dto.UpdateCourseRequest;
import com.lms.course_service.exception.CourseNotFoundException;
import com.lms.course_service.exception.UnauthorizedException;
import com.lms.course_service.exception.ValidationException;
import com.lms.course_service.model.Course;
import com.lms.course_service.model.Enrollment;
import com.lms.course_service.model.LectureContent;
import com.lms.course_service.repo.CourseRepository;
import com.lms.course_service.repo.EnrollmentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class CourseService {

    private static final Logger log = LoggerFactory.getLogger(CourseService.class);

    private static final Set<String> MATERIAL_CONTENT_TYPES = Set.of(
            "application/pdf",
            "image/jpeg",
            "image/png",
            "image/gif",
            "image/webp",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "text/plain"
    );

    private final CourseRepository courseRepo;
    private final EnrollmentRepository enrollmentRepo;
    private final CourseFileStorageService fileStorage;

    public CourseService(
            CourseRepository courseRepo,
            EnrollmentRepository enrollmentRepo,
            CourseFileStorageService fileStorage
    ) {
        this.courseRepo = courseRepo;
        this.enrollmentRepo = enrollmentRepo;
        this.fileStorage = fileStorage;
    }

    public Course createCourse(String instructorId, String instructorName, CreateCourseRequest req) {
        Course course = new Course(req.getTitle(), req.getDescription(), instructorId);
        course.setInstructorName(instructorName(instructorName));
        course.setThumbnailImageUrl(req.getThumbnailImageUrl());
        course.setLectureContents(mergeLectureContents(List.of(), req.getLectureContents()));
        Course saved = courseRepo.save(course);
        log.atInfo()
                .addKeyValue("event.action", "course_created")
                .addKeyValue("event.outcome", "success")
                .addKeyValue("course.id", saved.getId())
                .addKeyValue("user.id", instructorId)
                .log("Course created");
        return saved;
    }

    public Page<Course> listPublishedCourses(int page, int size) {
        return courseRepo.findByPublishedTrue(PageRequest.of(page, size));
    }

    public Page<Course> listInstructorCourses(String instructorId, int page, int size) {
        return courseRepo.findByInstructorId(instructorId, PageRequest.of(page, size));
    }

    public Course publishCourse(String instructorId, String courseId) {
        Course course = courseRepo.findById(courseId)
                .orElseThrow(() -> new CourseNotFoundException(courseId));

        if (!course.getInstructorId().equals(instructorId)) {
            throw new UnauthorizedException("Not your course");
        }

        course.setPublished(true);
        course.setModifiedAt(Instant.now());
        Course saved = courseRepo.save(course);
        log.atInfo()
                .addKeyValue("event.action", "course_published")
                .addKeyValue("event.outcome", "success")
                .addKeyValue("course.id", courseId)
                .addKeyValue("user.id", instructorId)
                .log("Course published");
        return saved;
    }

    public Course getCourseById(String userId, String courseId) {
        Course course = courseRepo.findById(courseId)
                .orElseThrow(() -> new CourseNotFoundException(courseId));

        if (!course.isPublished() && !course.getInstructorId().equals(userId)) {
            throw new UnauthorizedException("Forbidden");
        }

        return course;
    }

    public Course updateCourse(String instructorId, String instructorName, String courseId, UpdateCourseRequest req) {
        Course course = courseRepo.findById(courseId)
                .orElseThrow(() -> new CourseNotFoundException(courseId));

        if (!course.getInstructorId().equals(instructorId)) {
            throw new UnauthorizedException("Not your course");
        }

        course.setTitle(req.getTitle());
        course.setDescription(req.getDescription());
        course.setThumbnailImageUrl(req.getThumbnailImageUrl());
        course.setInstructorName(instructorName(instructorName));
        List<LectureContent> existing = course.getLectureContents() == null ? List.of() : course.getLectureContents();
        course.setLectureContents(mergeLectureContents(existing, req.getLectureContents()));
        course.setModifiedAt(Instant.now());
        return courseRepo.save(course);
    }

    public Course uploadThumbnail(String instructorId, String instructorName, String courseId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ValidationException("File is required");
        }
        Course course = courseRepo.findById(courseId)
                .orElseThrow(() -> new CourseNotFoundException(courseId));
        if (!course.getInstructorId().equals(instructorId)) {
            throw new UnauthorizedException("Not your course");
        }
        String ext;
        try {
            ext = CourseFileStorageService.thumbnailExtension(file.getOriginalFilename());
        } catch (IllegalArgumentException e) {
            throw new ValidationException(e.getMessage());
        }
        try {
            fileStorage.saveThumbnail(courseId, file, ext);
        } catch (IOException e) {
            log.atError().setCause(e).log("Failed to store thumbnail");
            throw new ValidationException("Failed to store thumbnail");
        }
        course.setThumbnailImageUrl("/courses/" + courseId + "/thumbnail");
        course.setInstructorName(instructorName(instructorName));
        course.setModifiedAt(Instant.now());
        return courseRepo.save(course);
    }

    public Course appendMaterialFromUpload(
            String instructorId,
            String instructorName,
            String courseId,
            String title,
            String description,
            MultipartFile file
    ) {
        if (file == null || file.isEmpty()) {
            throw new ValidationException("File is required");
        }
        if (title == null || title.isBlank()) {
            throw new ValidationException("Title is required");
        }
        if (description == null || description.isBlank()) {
            throw new ValidationException("Description is required");
        }
        Course course = courseRepo.findById(courseId)
                .orElseThrow(() -> new CourseNotFoundException(courseId));
        if (!course.getInstructorId().equals(instructorId)) {
            throw new UnauthorizedException("Not your course");
        }
        String normalizedType = normalizeMaterialContentType(file);
        if (!MATERIAL_CONTENT_TYPES.contains(normalizedType)) {
            throw new ValidationException("Unsupported file type for course material");
        }

        String materialId = UUID.randomUUID().toString();
        try {
            fileStorage.saveMaterialFile(courseId, materialId, file);
        } catch (IOException e) {
            log.atError().setCause(e).log("Failed to store material file");
            throw new ValidationException("Failed to store material file");
        }

        if (course.getLectureContents() == null) {
            course.setLectureContents(new ArrayList<>());
        }
        LectureContent lc = new LectureContent();
        lc.setMaterialId(materialId);
        lc.setTitle(title.trim());
        lc.setDescription(description.trim());
        lc.setContentType(normalizedType);
        lc.setContentUrl("/courses/" + courseId + "/materials/" + materialId + "/download");
        lc.setUploadedAt(Instant.now());
        course.getLectureContents().add(lc);
        course.setInstructorName(instructorName(instructorName));
        course.setModifiedAt(Instant.now());
        return courseRepo.save(course);
    }

    public ServedResource loadThumbnailForUser(String userId, String courseId) throws IOException {
        Course course = courseRepo.findById(courseId)
                .orElseThrow(() -> new CourseNotFoundException(courseId));
        assertCourseVisibleForAsset(userId, course);
        ServedResource res = fileStorage.resolveThumbnail(courseId);
        if (res == null || res.resource() == null || !res.resource().exists()) {
            throw new ValidationException("Thumbnail not available");
        }
        return res;
    }

    public ServedMaterial loadMaterialForUser(String userId, String courseId, String materialId) throws IOException {
        Course course = courseRepo.findById(courseId)
                .orElseThrow(() -> new CourseNotFoundException(courseId));
        LectureContent meta = assertMaterialDownloadAllowed(userId, course, materialId);
        ServedResource served = fileStorage.resolveMaterialFile(courseId, materialId, meta.getContentType());
        if (served == null || served.resource() == null || !served.resource().exists()) {
            throw new ValidationException("Material file missing");
        }
        String filename = filenameForMaterialDownload(meta);
        return new ServedMaterial(served.resource(), served.mediaType(), filename);
    }

    public record ServedMaterial(org.springframework.core.io.Resource resource,
                                 org.springframework.http.MediaType mediaType,
                                 String filename) {
    }

    public LectureContent findMaterialMeta(Course course, String materialId) {
        if (course.getLectureContents() == null) {
            return null;
        }
        return course.getLectureContents().stream()
                .filter(lc -> materialId.equals(lc.getMaterialId()))
                .findFirst()
                .orElse(null);
    }

    public String filenameForMaterialDownload(LectureContent lc) {
        String base = lc.getTitle() == null ? "download" : lc.getTitle().replaceAll("[^a-zA-Z0-9._-]+", "_");
        String ext = guessExtensionFromMime(lc.getContentType());
        if (ext != null && !base.toLowerCase(Locale.ROOT).endsWith("." + ext)) {
            return base + "." + ext;
        }
        return base;
    }

    private static String guessExtensionFromMime(String mime) {
        if (mime == null) {
            return "bin";
        }
        return switch (mime.toLowerCase(Locale.ROOT)) {
            case "application/pdf" -> "pdf";
            case "image/jpeg" -> "jpg";
            case "image/png" -> "png";
            case "image/gif" -> "gif";
            case "image/webp" -> "webp";
            case "application/msword" -> "doc";
            case "application/vnd.openxmlformats-officedocument.wordprocessingml.document" -> "docx";
            case "text/plain" -> "txt";
            default -> "bin";
        };
    }

    private void assertCourseVisibleForAsset(String userId, Course course) {
        if (!course.isPublished() && !course.getInstructorId().equals(userId)) {
            throw new UnauthorizedException("Forbidden");
        }
    }

    private LectureContent assertMaterialDownloadAllowed(String userId, Course course, String materialId) {
        LectureContent meta = findMaterialMeta(course, materialId);
        if (meta == null) {
            throw new ValidationException("Material not found");
        }
        if (course.getInstructorId().equals(userId)) {
            return meta;
        }
        if (!course.isPublished()) {
            throw new UnauthorizedException("Forbidden");
        }
        if (enrollmentRepo.findByCourseIdAndStudentId(course.getId(), userId).isEmpty()) {
            throw new UnauthorizedException("Enroll to access course materials");
        }
        return meta;
    }

    private static String normalizeMaterialContentType(MultipartFile file) {
        String ct = file.getContentType();
        if (ct != null && MATERIAL_CONTENT_TYPES.contains(ct.toLowerCase(Locale.ROOT))) {
            return ct.toLowerCase(Locale.ROOT);
        }
        String name = file.getOriginalFilename();
        String ext = CourseFileStorageService.extension(name);
        if (ext == null) {
            return "";
        }
        return switch (ext.toLowerCase(Locale.ROOT)) {
            case "pdf" -> "application/pdf";
            case "jpg", "jpeg" -> "image/jpeg";
            case "png" -> "image/png";
            case "gif" -> "image/gif";
            case "webp" -> "image/webp";
            case "doc" -> "application/msword";
            case "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            case "txt" -> "text/plain";
            default -> ct != null ? ct.toLowerCase(Locale.ROOT) : "";
        };
    }

    private static String instructorName(String raw) {
        return raw == null || raw.isBlank() ? "" : raw.trim();
    }

    private List<LectureContent> mergeLectureContents(
            List<LectureContent> existingList,
            List<LectureContentDto> incoming
    ) {
        if (incoming == null) {
            return List.of();
        }
        Map<String, LectureContent> byId = new HashMap<>();
        if (existingList != null) {
            for (LectureContent lc : existingList) {
                if (lc.getMaterialId() != null && !lc.getMaterialId().isBlank()) {
                    byId.put(lc.getMaterialId(), lc);
                }
            }
        }
        Set<String> usedNewIds = new HashSet<>();
        List<LectureContent> out = new ArrayList<>();
        for (LectureContentDto dto : incoming) {
            String mid = dto.getMaterialId();
            if (mid != null && !mid.isBlank() && byId.containsKey(mid)) {
                LectureContent old = byId.get(mid);
                LectureContent lc = new LectureContent();
                lc.setMaterialId(mid);
                lc.setTitle(dto.getTitle());
                lc.setContentType(dto.getContentType());
                lc.setContentUrl(dto.getContentUrl());
                lc.setDescription(dto.getDescription());
                lc.setDurationSeconds(dto.getDurationSeconds());
                lc.setUploadedAt(old.getUploadedAt() != null ? old.getUploadedAt() : Instant.now());
                out.add(lc);
            } else {
                LectureContent lc = new LectureContent();
                String newId = UUID.randomUUID().toString();
                while (usedNewIds.contains(newId) || byId.containsKey(newId)) {
                    newId = UUID.randomUUID().toString();
                }
                usedNewIds.add(newId);
                lc.setMaterialId(newId);
                lc.setTitle(dto.getTitle());
                lc.setContentType(dto.getContentType());
                lc.setContentUrl(dto.getContentUrl());
                lc.setDescription(dto.getDescription());
                lc.setDurationSeconds(dto.getDurationSeconds());
                lc.setUploadedAt(Instant.now());
                out.add(lc);
            }
        }
        return out;
    }

    public void deleteCourse(String instructorId, String courseId) {
        Course course = courseRepo.findById(courseId)
                .orElseThrow(() -> new CourseNotFoundException(courseId));

        if (!course.getInstructorId().equals(instructorId)) {
            throw new UnauthorizedException("Not your course");
        }

        enrollmentRepo.deleteByCourseId(courseId);
        fileStorage.deleteCourseFiles(courseId);
        courseRepo.delete(course);
        log.atInfo()
                .addKeyValue("event.action", "course_deleted")
                .addKeyValue("event.outcome", "success")
                .addKeyValue("course.id", courseId)
                .addKeyValue("user.id", instructorId)
                .log("Course deleted");
    }

    public Page<Enrollment> listCourseStudents(String instructorId, String courseId, int page, int size) {
        Course course = courseRepo.findById(courseId)
                .orElseThrow(() -> new CourseNotFoundException(courseId));

        if (!course.getInstructorId().equals(instructorId)) {
            throw new UnauthorizedException("Not your course");
        }

        return enrollmentRepo.findByCourseId(courseId, PageRequest.of(page, size));
    }

    public void enroll(String studentId, String courseId) {
        Course course = courseRepo.findById(courseId)
                .orElseThrow(() -> new CourseNotFoundException(courseId));

        if (!course.isPublished()) {
            throw new ValidationException("Course not published");
        }

        if (enrollmentRepo.findByCourseIdAndStudentId(courseId, studentId).isPresent()) {
            throw new ValidationException("Already enrolled");
        }

        enrollmentRepo.save(new Enrollment(courseId, studentId));
        log.atInfo()
                .addKeyValue("event.action", "student_enrolled")
                .addKeyValue("event.outcome", "success")
                .addKeyValue("course.id", courseId)
                .addKeyValue("user.id", studentId)
                .log("Student enrolled in course");
    }

    public Page<Enrollment> listStudentEnrollments(String studentId, int page, int size) {
        return enrollmentRepo.findByStudentId(studentId, PageRequest.of(page, size));
    }
}
