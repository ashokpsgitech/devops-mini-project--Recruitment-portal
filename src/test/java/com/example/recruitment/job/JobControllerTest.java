package com.example.recruitment.job;

import com.example.recruitment.entity.Department;
import com.example.recruitment.repository.DepartmentRepository;
import com.example.recruitment.repository.JobRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
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

    @Autowired
    private DepartmentRepository departmentRepository;

    @Test
    @DisplayName("Public job listing page should load successfully")
    void listJobsLoads() throws Exception {
        mockMvc.perform(get("/jobs"))
                .andExpect(status().isOk())
                .andExpect(view().name("jobs/list"))
                .andExpect(model().attributeExists("jobs"));
    }

    @Test
    @WithMockUser(roles = "RECRUITER")
    @DisplayName("New job form page should load for recruiter")
    void newJobFormLoads() throws Exception {
        mockMvc.perform(get("/jobs/new"))
                .andExpect(status().isOk())
                .andExpect(view().name("jobs/form"))
                .andExpect(model().attributeExists("job"));
    }

    @Test
    @WithMockUser(roles = "RECRUITER", username = "recruiter1")
    @DisplayName("Valid job submission should persist and redirect")
    void createJobSuccess() throws Exception {
        long initialCount = jobRepository.count();

        Department dept = departmentRepository.findAll().stream().findFirst()
                .orElseGet(() -> {
                    Department d = new Department();
                    d.setName("Platform Engineering");
                    return departmentRepository.save(d);
                });

        mockMvc.perform(post("/jobs")
                        .with(csrf())
                        .param("title", "Senior Cloud Engineer")
                        .param("departmentId", String.valueOf(dept.getId()))
                        .param("employmentType", "FULL_TIME")
                        .param("location", "Remote")
                        .param("description", "Responsible for Azure cloud architecture and CI/CD pipelines.")
                        .param("requirements", "Java 21, Spring Boot 3, Azure, Docker, Kubernetes"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/recruiter/jobs"))
                .andExpect(flash().attributeExists("success"));

        assertEquals(initialCount + 1, jobRepository.count());
    }

    @Test
    @WithMockUser(roles = "RECRUITER")
    @DisplayName("Invalid job submission with blank fields should redisplay form with validation errors")
    void createJobValidationFailure() throws Exception {
        mockMvc.perform(post("/jobs")
                        .with(csrf())
                        .param("title", "")
                        .param("location", "")
                        .param("description", ""))
                .andExpect(status().isOk())
                .andExpect(view().name("jobs/form"))
                .andExpect(model().hasErrors());
    }
}
