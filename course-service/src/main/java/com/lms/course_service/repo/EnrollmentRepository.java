package com.lms.course_service.repo;

import com.lms.course_service.model.Enrollment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface EnrollmentRepository extends MongoRepository<Enrollment, String> {
    Optional<Enrollment> findByCourseIdAndStudentId(String courseId, String studentId);
    Page<Enrollment> findByStudentId(String studentId, Pageable pageable);
    Page<Enrollment> findByCourseId(String courseId, Pageable pageable);
    void deleteByCourseId(String courseId);
    long countByCourseId(String courseId);
}
