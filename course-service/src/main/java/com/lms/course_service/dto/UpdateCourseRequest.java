package com.lms.course_service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.Valid;

import java.util.ArrayList;
import java.util.List;

public class UpdateCourseRequest {

    @NotBlank
    @Size(min = 3, max = 120)
    private String title;

    @NotBlank
    @Size(min = 3, max = 2000)
    private String description;

    @Size(max = 2000)
    private String thumbnailImageUrl;

    @Valid
    private List<LectureContentDto> lectureContents = new ArrayList<>();

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

    public String getThumbnailImageUrl() {
        return thumbnailImageUrl;
    }

    public void setThumbnailImageUrl(String thumbnailImageUrl) {
        this.thumbnailImageUrl = thumbnailImageUrl;
    }

    public List<LectureContentDto> getLectureContents() {
        return lectureContents;
    }

    public void setLectureContents(List<LectureContentDto> lectureContents) {
        this.lectureContents = lectureContents == null ? new ArrayList<>() : lectureContents;
    }
}
