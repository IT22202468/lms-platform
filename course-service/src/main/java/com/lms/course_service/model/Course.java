package com.lms.course_service.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Document("courses")
public class Course {

    @Id
    private String id;

    private String title;
    private String description;
    private String instructorId;
    /** Denormalized display (typically instructor email from gateway). */
    private String instructorName;
    private String thumbnailImageUrl;
    private List<LectureContent> lectureContents = new ArrayList<>();
    private boolean published = false;
    private Instant createdAt = Instant.now();
    private Instant modifiedAt = Instant.now();

    public Course() { }

    public Course(String title, String description, String instructorId) {
        this.title = title;
        this.description = description;
        this.instructorId = instructorId;
        this.createdAt = Instant.now();
        this.modifiedAt = Instant.now();
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

    public String getInstructorName() {
        return instructorName;
    }

    public void setInstructorName(String instructorName) {
        this.instructorName = instructorName;
    }

    public String getThumbnailImageUrl() {
        return thumbnailImageUrl;
    }

    public void setThumbnailImageUrl(String thumbnailImageUrl) {
        this.thumbnailImageUrl = thumbnailImageUrl;
    }

    public List<LectureContent> getLectureContents() {
        return lectureContents;
    }

    public void setLectureContents(List<LectureContent> lectureContents) {
        this.lectureContents = lectureContents == null ? new ArrayList<>() : lectureContents;
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

    public Instant getModifiedAt() {
        return modifiedAt;
    }

    public void setModifiedAt(Instant modifiedAt) {
        this.modifiedAt = modifiedAt;
    }

    public Instant getUpdatedAt() {
        return modifiedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.modifiedAt = updatedAt;
    }
}
