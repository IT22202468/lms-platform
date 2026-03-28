package com.lms.course_service.dto;

import java.time.Instant;

public class CourseResponse {

    private String id;
    private String title;
    private String description;
    private String instructorId;
    private boolean published;
    private Instant createdAt;

    public CourseResponse() { }

    public CourseResponse(String id, String title, String description, String instructorId, boolean published, Instant createdAt) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.instructorId = instructorId;
        this.published = published;
        this.createdAt = createdAt;
    }

    // getters/setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getInstructorId() { return instructorId; }
    public void setInstructorId(String instructorId) { this.instructorId = instructorId; }

    public boolean isPublished() { return published; }
    public void setPublished(boolean published) { this.published = published; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
