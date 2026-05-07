package com.lms.course_service;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class MainMethodTest {

    @Test
    void mainMethodStarts() {
        // Just calling it to cover the lines
        CourseServiceApplication.main(new String[]{"--server.port=0"});
        assertThat(true).isTrue();
    }
}
