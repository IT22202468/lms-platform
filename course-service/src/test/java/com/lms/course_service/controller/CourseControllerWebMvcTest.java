package com.lms.course_service.controller;

import com.lms.course_service.config.SecurityConfig;
import com.lms.course_service.model.Course;
import com.lms.course_service.security.IdentityExtractor;
import com.lms.course_service.security.RequestIdentity;
import com.lms.course_service.service.CourseService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = CourseController.class)
@Import({GlobalExceptionHandler.class, SecurityConfig.class})
class CourseControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CourseService courseService;

    @MockitoBean
    private IdentityExtractor identityExtractor;

    @Test
    void listCourses_returnsPage() throws Exception {
        Course course = new Course("Intro to Testing", "A course about tests", "inst-1");
        course.setId("c1");
        course.setPublished(true);
        course.setCreatedAt(Instant.parse("2026-01-01T00:00:00Z"));
        when(courseService.listPublishedCourses(0, 10))
                .thenReturn(new PageImpl<>(List.of(course), PageRequest.of(0, 10), 1));

        mockMvc.perform(get("/courses").param("page", "0").param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].id").value("c1"))
                .andExpect(jsonPath("$.items[0].title").value("Intro to Testing"))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(10))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.totalPages").value(1));
    }

    @Test
    void createCourse_instructor_returns201() throws Exception {
        when(identityExtractor.extract(any())).thenReturn(new RequestIdentity("i1", "INSTRUCTOR"));
        Course saved = new Course("Valid title here", "Valid description here ok", "i1");
        saved.setId("new-c");
        saved.setPublished(false);
        saved.setCreatedAt(Instant.parse("2026-02-01T00:00:00Z"));
        when(courseService.createCourse(eq("i1"), any())).thenReturn(saved);

        String body = """
                {"title":"Valid title here","description":"Valid description here ok"}
                """;

        mockMvc.perform(post("/courses").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("new-c"))
                .andExpect(jsonPath("$.instructorId").value("i1"));
    }

    @Test
    void createCourse_student_returns403() throws Exception {
        when(identityExtractor.extract(any())).thenReturn(new RequestIdentity("s1", "STUDENT"));

        String body = """
                {"title":"Valid title here","description":"Valid description here ok"}
                """;

        mockMvc.perform(post("/courses").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isForbidden());
    }

    @Test
    void createCourse_invalidBody_returns400() throws Exception {
        when(identityExtractor.extract(any())).thenReturn(new RequestIdentity("i1", "INSTRUCTOR"));

        String body = """
                {"title":"ab","description":"no"}
                """;

        mockMvc.perform(post("/courses").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getCourseById_returns200() throws Exception {
        when(identityExtractor.extract(any())).thenReturn(new RequestIdentity("u1", "STUDENT"));
        Course course = new Course("T", "D", "inst");
        course.setId("c1");
        course.setPublished(true);
        course.setCreatedAt(Instant.parse("2026-01-01T00:00:00Z"));
        when(courseService.getCourseById("u1", "c1")).thenReturn(course);

        mockMvc.perform(get("/courses/c1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("c1"));
    }

    @Test
    void getCourseById_missingUserHeader_returns401() throws Exception {
        when(identityExtractor.extract(any())).thenThrow(new SecurityException("Missing X-User-Id"));

        mockMvc.perform(get("/courses/c1"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").exists());
    }
}
