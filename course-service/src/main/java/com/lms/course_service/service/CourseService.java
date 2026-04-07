package com.lms.course_service.service;

import com.lms.course_service.dto.CreateCourseRequest;
import com.lms.course_service.dto.UpdateCourseRequest;
import com.lms.course_service.model.Course;
import com.lms.course_service.model.Enrollment;
import com.lms.course_service.repo.CourseRepository;
import com.lms.course_service.repo.EnrollmentRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
public class CourseService {

    private final CourseRepository courseRepo;
    private final EnrollmentRepository enrollmentRepo;

    public CourseService(CourseRepository courseRepo, EnrollmentRepository enrollmentRepo) {
        this.courseRepo = courseRepo;
        this.enrollmentRepo = enrollmentRepo;
    }

    public Course createCourse(String instructorId, CreateCourseRequest req){
        Course course = new Course(req.getTitle(), req.getDescription(), instructorId);
        return courseRepo.save(course);
    }

    public List<Course> listPublishedCourses() {
        return courseRepo.findByPublishedTrue();
    }

    public List<Course> listInstructorCourses(String instructorId) {
        return courseRepo.findByInstructorId(instructorId);
    }

    public Course publishCourse(String instructorId, String courseId) {
        Course course = courseRepo.findById(courseId)
                .orElseThrow(() -> new IllegalArgumentException("Course not found"));

        if (!course.getInstructorId().equals(instructorId)) {
            throw new SecurityException("Not your course");
        }

        course.setPublished(true);
        course.setUpdatedAt(Instant.now());
        return courseRepo.save(course);
    }

    public Course getCourseById(String userId, String courseId) {
        Course course = courseRepo.findById(courseId)
                .orElseThrow(() -> new IllegalArgumentException("Course not found"));

        if (!course.isPublished() && !course.getInstructorId().equals(userId)) {
            throw new SecurityException("Forbidden");
        }

        return course;
    }

    public Course updateCourse(String instructorId, String courseId, UpdateCourseRequest req) {
        Course course = courseRepo.findById(courseId)
                .orElseThrow(() -> new IllegalArgumentException("Course not found"));

        if (!course.getInstructorId().equals(instructorId)) {
            throw new SecurityException("Not your course");
        }

        course.setTitle(req.getTitle());
        course.setDescription(req.getDescription());
        course.setUpdatedAt(Instant.now());
        return courseRepo.save(course);
    }

    public void deleteCourse(String instructorId, String courseId) {
        Course course = courseRepo.findById(courseId)
                .orElseThrow(() -> new IllegalArgumentException("Course not found"));

        if (!course.getInstructorId().equals(instructorId)) {
            throw new SecurityException("Not your course");
        }

        enrollmentRepo.deleteByCourseId(courseId);
        courseRepo.delete(course);
    }

    public List<Enrollment> listCourseStudents(String instructorId, String courseId) {
        Course course = courseRepo.findById(courseId)
                .orElseThrow(() -> new IllegalArgumentException("Course not found"));

        if (!course.getInstructorId().equals(instructorId)) {
            throw new SecurityException("Not your course");
        }

        return enrollmentRepo.findByCourseId(courseId);
    }

    public void enroll(String studentId, String courseId) {
        Course course = courseRepo.findById(courseId)
                .orElseThrow(() -> new IllegalArgumentException("Course not found"));

        if (!course.isPublished()) {
            throw new IllegalArgumentException("Course not published");
        }

        if (enrollmentRepo.findByCourseIdAndStudentId(courseId, studentId).isPresent()) {
            throw new IllegalArgumentException("Already enrolled");
        }

        enrollmentRepo.save(new Enrollment(courseId, studentId));
    }

    public List<Enrollment> listStudentEnrollments(String studentId) {
        return enrollmentRepo.findByStudentId(studentId);
    }
}
