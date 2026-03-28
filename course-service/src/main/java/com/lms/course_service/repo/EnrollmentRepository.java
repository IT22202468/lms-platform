package com.lms.course_service.repo;

import com.lms.course_service.model.Enrollment;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface EnrollmentRepository extends MongoRepository<Enrollment, String> {
    Optional<Enrollment> findByCourseIdAndStudentId(String courseId, String studentId);
    List<Enrollment> findByStudentId(String studentId);
    long countByCourseId(String courseId);
}
