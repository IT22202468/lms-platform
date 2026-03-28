package com.lms.course_service.repo;

import com.lms.course_service.model.Course;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface CourseRepository extends MongoRepository<Course, String> {
    List<Course> findByPublishedTrue();
    List<Course> findByInstructorId(String instructorId);
}
