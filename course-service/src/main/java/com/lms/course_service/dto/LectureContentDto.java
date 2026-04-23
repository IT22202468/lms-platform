package com.lms.course_service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public class LectureContentDto {

    @NotBlank
    @Size(min = 2, max = 200)
    private String title;

    @NotBlank
    @Size(min = 2, max = 30)
    private String contentType;

    @NotBlank
    @Size(min = 5, max = 2000)
    private String contentUrl;

    @Size(max = 1000)
    private String description;

    @PositiveOrZero
    private Long durationSeconds;

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
