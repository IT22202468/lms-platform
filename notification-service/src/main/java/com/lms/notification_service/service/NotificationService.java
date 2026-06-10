package com.lms.notification_service.service;

import com.lms.notification_service.dto.UserRegisteredEvent;
import com.lms.notification_service.dto.CourseEnrolledEvent;
import org.springframework.stereotype.Service;

@Service
public class NotificationService {

    private final EmailSender emailSender;
    
    public NotificationService(EmailSender emailSender) {
        this.emailSender = emailSender;
    }

    public void handleUserRegistered(UserRegisteredEvent ev) {
        String to = ev.getEmail();
        String subject = "Welcome to OtterSpaceLearn";
        String body = String.format("Hi — welcome to OtterSpace! Your account (%s) was created at %s.\n\nHappy learning Otter!",
                ev.getEmail(), ev.getCreatedAt());
        emailSender.send(to, subject, body);
    }

    public void handleCourseEnrolled(CourseEnrolledEvent ev) {
        String to = ev.getStudentEmail();
        String subject = "Course enrollment confirmed";
        String body = String.format("You were enrolled in course %s on %s.\n\nGood luck Otter!",
                ev.getCourseId(), ev.getEnrolledAt());
        emailSender.send(to, subject, body);
    }
}
