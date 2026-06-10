package com.lms.notification_service.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lms.notification_service.dto.CourseEnrolledEvent;
import com.lms.notification_service.dto.UserRegisteredEvent;
import com.lms.notification_service.service.NotificationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.concurrent.TimeUnit;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

@SpringBootTest
@EmbeddedKafka(partitions = 1, topics = {"user.registered", "course.enrolled"})
@ActiveProfiles("test")
class EventConsumerIntegrationTest {

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private NotificationService notificationService;

    @Test
    void handleUserRegistered_consumesMessage() throws Exception {
        UserRegisteredEvent event = new UserRegisteredEvent("u1", "test@example.com", Instant.now());
        String payload = objectMapper.writeValueAsString(event);

        kafkaTemplate.send("user.registered", payload);

        verify(notificationService, timeout(5000)).handleUserRegistered(any(UserRegisteredEvent.class));
    }

    @Test
    void handleCourseEnrolled_consumesMessage() throws Exception {
        CourseEnrolledEvent event = new CourseEnrolledEvent("u1", "stu@example.com", "c1", Instant.now());
        String payload = objectMapper.writeValueAsString(event);

        kafkaTemplate.send("course.enrolled", payload);

        verify(notificationService, timeout(5000)).handleCourseEnrolled(any(CourseEnrolledEvent.class));
    }
}
