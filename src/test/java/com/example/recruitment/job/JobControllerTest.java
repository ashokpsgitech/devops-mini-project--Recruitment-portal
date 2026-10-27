package com.example.recruitment.job;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class JobControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JobRepository jobRepository;

    @Test
    @DisplayName("Root URL should redirect to /jobs")
    void rootRedirectsToJobs() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/jobs"));
    }

    @Test
    @DisplayName("Job listing page should load successfully")
    void listJobsLoads() throws Exception {
        mockMvc.perform(get("/jobs"))
                .andExpect(status().isOk())
                .andExpect(view().name("jobs"))
                .andExpect(model().attributeExists("jobs"));
    }

    @Test
    @DisplayName("New job form page should load successfully")
    void newJobFormLoads() throws Exception {
        mockMvc.perform(get("/jobs/new"))
                .andExpect(status().isOk())
                .andExpect(view().name("job-form"))
                .andExpect(model().attributeExists("job"));
    }

    @Test
    @DisplayName("Valid job submission should persist and redirect")
    void createJobSuccess() throws Exception {
        long initialCount = jobRepository.count();

        mockMvc.perform(post("/jobs")
                        .param("title", "Senior Cloud Engineer")
                        .param("department", "Infrastructure")
                        .param("location", "Remote")
                        .param("description", "Responsible for Azure cloud architecture and CI/CD pipelines."))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/jobs"))
                .andExpect(flash().attribute("message", "Job published successfully!"));

        assertEquals(initialCount + 1, jobRepository.count());
    }

    @Test
    @DisplayName("Invalid job submission with blank fields should redisplay form with validation errors")
    void createJobValidationFailure() throws Exception {
        mockMvc.perform(post("/jobs")
                        .param("title", "")
                        .param("department", "")
                        .param("location", "")
                        .param("description", ""))
                .andExpect(status().isOk())
                .andExpect(view().name("job-form"))
                .andExpect(model().hasErrors());
    }
}
