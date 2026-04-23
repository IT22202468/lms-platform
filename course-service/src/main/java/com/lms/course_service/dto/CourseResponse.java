package com.lms.course_service.dto;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class CourseResponse {

    private String id;
    private String title;
    private String description;
    private String instructorId;
    private String thumbnailImageUrl;
    private List<LectureContentDto> lectureContents = new ArrayList<>();
    private boolean published;
    private Instant createdAt;
    private Instant modifiedAt;

    public CourseResponse() { }

    public CourseResponse(
            String id,
            String title,
            String description,
            String instructorId,
            String thumbnailImageUrl,
            List<LectureContentDto> lectureContents,
            boolean published,
            Instant createdAt,
            Instant modifiedAt
    ) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.instructorId = instructorId;
        this.thumbnailImageUrl = thumbnailImageUrl;
        this.lectureContents = lectureContents;
        this.published = published;
        this.createdAt = createdAt;
        this.modifiedAt = modifiedAt;
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

    public String getThumbnailImageUrl() { return thumbnailImageUrl; }
    public void setThumbnailImageUrl(String thumbnailImageUrl) { this.thumbnailImageUrl = thumbnailImageUrl; }

    public List<LectureContentDto> getLectureContents() { return lectureContents; }
    public void setLectureContents(List<LectureContentDto> lectureContents) { this.lectureContents = lectureContents; }

    public boolean isPublished() { return published; }
    public void setPublished(boolean published) { this.published = published; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getModifiedAt() { return modifiedAt; }
    public void setModifiedAt(Instant modifiedAt) { this.modifiedAt = modifiedAt; }
}
