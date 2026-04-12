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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class CourseService {

    private static final Logger log = LoggerFactory.getLogger(CourseService.class);

    private final CourseRepository courseRepo;
    private final EnrollmentRepository enrollmentRepo;

    public CourseService(CourseRepository courseRepo, EnrollmentRepository enrollmentRepo) {
        this.courseRepo = courseRepo;
        this.enrollmentRepo = enrollmentRepo;
    }

    public Course createCourse(String instructorId, CreateCourseRequest req){
        Course course = new Course(req.getTitle(), req.getDescription(), instructorId);
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
        course.setUpdatedAt(Instant.now());
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

    public Course updateCourse(String instructorId, String courseId, UpdateCourseRequest req) {
        Course course = courseRepo.findById(courseId)
                .orElseThrow(() -> new CourseNotFoundException(courseId));

        if (!course.getInstructorId().equals(instructorId)) {
            throw new UnauthorizedException("Not your course");
        }

        course.setTitle(req.getTitle());
        course.setDescription(req.getDescription());
        course.setUpdatedAt(Instant.now());
        return courseRepo.save(course);
    }

    public void deleteCourse(String instructorId, String courseId) {
        Course course = courseRepo.findById(courseId)
                .orElseThrow(() -> new CourseNotFoundException(courseId));

        if (!course.getInstructorId().equals(instructorId)) {
            throw new UnauthorizedException("Not your course");
        }

        enrollmentRepo.deleteByCourseId(courseId);
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
