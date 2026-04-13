package com.lms.course_service.dto;

import java.time.Instant;

public class CourseStudentResponse {

    private String studentId;
    private Instant enrolledAt;

    public CourseStudentResponse() {
    }

    public CourseStudentResponse(String studentId, Instant enrolledAt) {
        this.studentId = studentId;
        this.enrolledAt = enrolledAt;
    }

    public String getStudentId() {
        return studentId;
    }

    public void setStudentId(String studentId) {
        this.studentId = studentId;
    }

    public Instant getEnrolledAt() {
        return enrolledAt;
    }

    public void setEnrolledAt(Instant enrolledAt) {
        this.enrolledAt = enrolledAt;
    }
}
