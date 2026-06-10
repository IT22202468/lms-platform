package com.lms.notification_service.service;

import com.lms.notification_service.dto.CourseEnrolledEvent;
import com.lms.notification_service.dto.UserRegisteredEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private EmailSender emailSender;

    @InjectMocks
    private NotificationService notificationService;

    @Test
    void handleUserRegistered_sendsEmail() {
        UserRegisteredEvent event = new UserRegisteredEvent("u1", "test@example.com", Instant.now());
        
        notificationService.handleUserRegistered(event);

        verify(emailSender).send(
            eq("test@example.com"),
            contains("Welcome"),
            contains("test@example.com")
        );
    }

    @Test
    void handleCourseEnrolled_sendsEmail() {
        CourseEnrolledEvent event = new CourseEnrolledEvent("c1", "u1", "student@example.com", Instant.now());

        notificationService.handleCourseEnrolled(event);

        verify(emailSender).send(
            eq("student@example.com"),
            contains("enrollment"),
            contains("c1")
        );
    }
}
