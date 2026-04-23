package com.lms.course_service.model;

public class LectureContent {

    private String title;
    private String contentType;
    private String contentUrl;
    private String description;
    private Long durationSeconds;

    public LectureContent() {
    }

    public LectureContent(String title, String contentType, String contentUrl, String description, Long durationSeconds) {
        this.title = title;
        this.contentType = contentType;
        this.contentUrl = contentUrl;
        this.description = description;
        this.durationSeconds = durationSeconds;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public String getContentUrl() {
        return contentUrl;
    }

    public void setContentUrl(String contentUrl) {
        this.contentUrl = contentUrl;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Long getDurationSeconds() {
        return durationSeconds;
    }

    public void setDurationSeconds(Long durationSeconds) {
        this.durationSeconds = durationSeconds;
    }
}
