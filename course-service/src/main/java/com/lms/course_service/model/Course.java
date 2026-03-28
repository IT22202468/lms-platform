package com.lms.course_service.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document("courses")
public class Course {

    @Id
    private String id;

    private String title;
    private String description;
    private String instructorId;
    private boolean published = false;
    private Instant createdAt = Instant.now();
    private Instant updatedAt = Instant.now();

    public Course() { }

    public Course(String title, String description, String instructorId) {
        this.title = title;
        this.description = description;
        this.instructorId = instructorId;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public String getId() {
        return id;
    }
    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }
    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }
    public void setDescription(String description) {
        this.description = description;
    }

    public String getInstructorId() {
        return instructorId;
    }
    public void setInstructorId(String instructorId) {
        this.instructorId = instructorId;
    }

    public boolean isPublished() {
        return published;
    }
    public void setPublished(boolean published) {
        this.published = published;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
