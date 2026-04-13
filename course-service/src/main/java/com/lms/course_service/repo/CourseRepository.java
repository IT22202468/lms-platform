package com.lms.course_service.repo;

import com.lms.course_service.model.Course;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface CourseRepository extends MongoRepository<Course, String> {
    Page<Course> findByPublishedTrue(Pageable pageable);
    Page<Course> findByInstructorId(String instructorId, Pageable pageable);
    boolean existsByIdAndInstructorId(String id, String instructorId);
}
