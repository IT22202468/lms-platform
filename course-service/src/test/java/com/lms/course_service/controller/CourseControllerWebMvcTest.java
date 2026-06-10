package com.lms.course_service.controller;

import com.lms.course_service.config.SecurityConfig;
import com.lms.course_service.model.Course;
import com.lms.course_service.security.IdentityExtractor;
import com.lms.course_service.security.RequestIdentity;
import com.lms.course_service.service.CourseService;
import com.lms.course_service.service.CourseService.ServedMaterial;
import com.lms.course_service.dto.ServedResource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.time.Instant;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = CourseController.class)
@Import({GlobalExceptionHandler.class, SecurityConfig.class})
class CourseControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CourseService courseService;

    @MockBean
    private IdentityExtractor identityExtractor;

    @MockBean
    private com.lms.course_service.security.JwtService jwtService;

    @Test
    void listCourses_returnsPage() throws Exception {
        when(identityExtractor.extract(any())).thenReturn(new RequestIdentity("u1", "STUDENT", "student@example.com"));
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
        when(identityExtractor.extract(any())).thenReturn(new RequestIdentity("i1", "INSTRUCTOR", "instr@example.com"));
        Course saved = new Course("Valid title here", "Valid description here ok", "i1");
        saved.setId("new-c");
        saved.setPublished(false);
        saved.setCreatedAt(Instant.parse("2026-02-01T00:00:00Z"));
        when(courseService.createCourse(eq("i1"), anyString(), any())).thenReturn(saved);

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
        when(identityExtractor.extract(any())).thenReturn(new RequestIdentity("s1", "STUDENT", "stu@example.com"));

        String body = """
                {"title":"Valid title here","description":"Valid description here ok"}
                """;

        mockMvc.perform(post("/courses").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isForbidden());
    }

    @Test
    void createCourse_invalidBody_returns400() throws Exception {
        when(identityExtractor.extract(any())).thenReturn(new RequestIdentity("i1", "INSTRUCTOR", "instr@example.com"));

        String body = """
                {"title":"ab","description":"no"}
                """;

        mockMvc.perform(post("/courses").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getCourseById_returns200() throws Exception {
        when(identityExtractor.extract(any())).thenReturn(new RequestIdentity("u1", "STUDENT", "stu@example.com"));
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

    @Test
    void updateCourse_returns200() throws Exception {
        when(identityExtractor.extract(any())).thenReturn(new RequestIdentity("i1", "INSTRUCTOR", "instr@example.com"));
        Course updated = new Course("New Title", "New Desc", "i1");
        updated.setId("c1");
        updated.setCreatedAt(Instant.parse("2026-01-01T00:00:00Z"));
        when(courseService.updateCourse(eq("i1"), anyString(), eq("c1"), any())).thenReturn(updated);

        String body = """
                {"title":"New Title","description":"New Desc"}
                """;

        mockMvc.perform(put("/courses/c1").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("New Title"));
    }

    @Test
    void publish_returns200() throws Exception {
        when(identityExtractor.extract(any())).thenReturn(new RequestIdentity("i1", "INSTRUCTOR", "instr@example.com"));
        Course published = new Course("Title", "Desc", "i1");
        published.setId("c1");
        published.setPublished(true);
        published.setCreatedAt(Instant.parse("2026-01-01T00:00:00Z"));
        when(courseService.publishCourse("i1", "c1")).thenReturn(published);

        mockMvc.perform(put("/courses/c1/publish"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.published").value(true));
    }

    @Test
    void myCourses_returnsPage() throws Exception {
        when(identityExtractor.extract(any())).thenReturn(new RequestIdentity("i1", "INSTRUCTOR", "instr@example.com"));
        Course course = new Course("T", "D", "i1");
        course.setId("c1");
        course.setCreatedAt(Instant.parse("2026-01-01T00:00:00Z"));
        when(courseService.listInstructorCourses(eq("i1"), anyInt(), anyInt()))
                .thenReturn(new PageImpl<>(List.of(course)));

        mockMvc.perform(get("/instructor/courses"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].id").value("c1"));
    }

    @Test
    void deleteCourse_returns200() throws Exception {
        when(identityExtractor.extract(any())).thenReturn(new RequestIdentity("i1", "INSTRUCTOR", "instr@example.com"));
        doNothing().when(courseService).deleteCourse("i1", "c1");

        mockMvc.perform(delete("/courses/c1"))
                .andExpect(status().isOk());
    }

        @Test
        void uploadThumbnail_returns200() throws Exception {
                when(identityExtractor.extract(any())).thenReturn(new RequestIdentity("i1", "INSTRUCTOR", "instr@example.com"));
                Course saved = new Course("Title", "Desc", "i1");
                saved.setId("c1");
                when(courseService.uploadThumbnail(eq("i1"), anyString(), eq("c1"), any())).thenReturn(saved);

                mockMvc.perform(multipart("/courses/c1/thumbnail")
                                                .file(new org.springframework.mock.web.MockMultipartFile("file", "thumb.png", "image/png", "img".getBytes())))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.id").value("c1"));
        }

        @Test
        void uploadMaterial_returns200() throws Exception {
                when(identityExtractor.extract(any())).thenReturn(new RequestIdentity("i1", "INSTRUCTOR", "instr@example.com"));
                Course saved = new Course("Title", "Desc", "i1");
                saved.setId("c1");
                when(courseService.appendMaterialFromUpload(eq("i1"), anyString(), eq("c1"), eq("Lecture title"), eq("Lecture description"), any()))
                                .thenReturn(saved);

                mockMvc.perform(multipart("/courses/c1/materials")
                                                .file(new org.springframework.mock.web.MockMultipartFile("file", "lesson.pdf", "application/pdf", "pdf".getBytes()))
                                                .param("title", "Lecture title")
                                                .param("description", "Lecture description"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.id").value("c1"));
        }

        @Test
        void streamThumbnail_returnsImageWithCacheHeader() throws Exception {
                when(identityExtractor.extract(any())).thenReturn(new RequestIdentity("u1", "STUDENT", "student@example.com"));
                ServedResource served = new ServedResource(new ByteArrayResource("img".getBytes()), MediaType.IMAGE_PNG);
                when(courseService.loadThumbnailForUser("u1", "c1")).thenReturn(served);

                mockMvc.perform(get("/courses/c1/thumbnail"))
                                .andExpect(status().isOk())
                                .andExpect(content().contentType(MediaType.IMAGE_PNG))
                                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "private, max-age=3600"));
        }

        @Test
        void downloadMaterial_returnsAttachmentHeader() throws Exception {
                when(identityExtractor.extract(any())).thenReturn(new RequestIdentity("u1", "STUDENT", "student@example.com"));
                ServedMaterial served = new ServedMaterial(new ByteArrayResource("pdf".getBytes()), MediaType.APPLICATION_PDF, "lesson.pdf");
                when(courseService.loadMaterialForUser("u1", "c1", "m1")).thenReturn(served);

                mockMvc.perform(get("/courses/c1/materials/m1/download"))
                                .andExpect(status().isOk())
                                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, containsString("lesson.pdf")));
        }

    @Test
    void enroll_returns201() throws Exception {
        when(identityExtractor.extract(any())).thenReturn(new RequestIdentity("s1", "STUDENT", "student@example.com"));
        doNothing().when(courseService).enroll("s1", "c1");

        mockMvc.perform(post("/courses/c1/enroll"))
                .andExpect(status().isCreated());
    }


    @Test
    void myEnrollments_returnsPage() throws Exception {
        when(identityExtractor.extract(any())).thenReturn(new RequestIdentity("s1", "STUDENT", "student@example.com"));
        when(courseService.listStudentEnrollments(eq("s1"), anyInt(), anyInt()))
                .thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/student/enrollments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isArray());
    }

    @Test
    void listCourseStudents_returnsList() throws Exception {
        when(identityExtractor.extract(any())).thenReturn(new RequestIdentity("i1", "INSTRUCTOR", "instr@example.com"));
        when(courseService.listCourseStudents(eq("i1"), eq("c1"), anyInt(), anyInt()))
                .thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/courses/c1/students"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isArray());
    }
}
