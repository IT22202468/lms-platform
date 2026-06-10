package com.lms.notification_service.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lms.notification_service.dto.CourseEnrolledEvent;
import com.lms.notification_service.dto.UserRegisteredEvent;
import com.lms.notification_service.service.NotificationService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class EventConsumer {

    private final ObjectMapper objectMapper;
    private final NotificationService notificationService;

    public EventConsumer(ObjectMapper objectMapper, NotificationService notificationService) {
        this.objectMapper = objectMapper;
        this.notificationService = notificationService;
    }

    @KafkaListener(topics = "user.registered", groupId = "notification-service-group")
    public void handleUserRegistered(String payload) throws Exception {
        UserRegisteredEvent ev = objectMapper.readValue(payload, UserRegisteredEvent.class);
        notificationService.handleUserRegistered(ev);
    }

    @KafkaListener(topics = "course.enrolled", groupId = "notification-service-group")
    public void handleCourseEnrolled(String payload) throws Exception {
        CourseEnrolledEvent ev = objectMapper.readValue(payload, CourseEnrolledEvent.class);
        notificationService.handleCourseEnrolled(ev);
    }
}
