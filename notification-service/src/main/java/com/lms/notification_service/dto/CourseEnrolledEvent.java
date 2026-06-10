package com.lms.notification_service.dto;

import java.time.Instant;

public class CourseEnrolledEvent {

    private String courseId;
    private String studentId;
    private String studentEmail;
    private Instant enrolledAt;

    public CourseEnrolledEvent() {}

    public CourseEnrolledEvent(String courseId, String studentId, String studentEmail, Instant enrolledAt) {
        this.courseId = courseId;
        this.studentId = studentId;
        this.studentEmail = studentEmail;
        this.enrolledAt = enrolledAt;
    }
    
    public String getCourseId() { return courseId; }
    public void setCourseId(String courseId) { this.courseId = courseId; }

    public String getStudentId() { return studentId; }
    public void setStudentId(String studentId) { this.studentId = studentId; }

    public String getStudentEmail() { return studentEmail; }
    public void setStudentEmail(String studentEmail) { this.studentEmail = studentEmail; }

    public Instant getEnrolledAt() { return enrolledAt; }
    public void setEnrolledAt(Instant enrolledAt) { this.enrolledAt = enrolledAt; }
}
