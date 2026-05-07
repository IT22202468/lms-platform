package com.lms.course_service.dto;

import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;

public record ServedResource(Resource resource, MediaType mediaType) {
}
