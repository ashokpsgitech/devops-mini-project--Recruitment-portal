package com.example.recruitment.controller;

import com.example.recruitment.dto.JobRequest;
import com.example.recruitment.entity.Department;
import com.example.recruitment.entity.Job;
import com.example.recruitment.repository.DepartmentRepository;
import com.example.recruitment.repository.JobRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class JobRestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("GET /api/v1/jobs should return paginated list of published jobs in JSON")
    void testListJobsJson() throws Exception {
        mockMvc.perform(get("/api/v1/jobs")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    @WithMockUser(roles = "RECRUITER", username = "recruiter1")
    @DisplayName("POST /api/v1/jobs should create and return new job as JSON")
    void testCreateJobJson() throws Exception {
        Department dept = departmentRepository.findAll().stream().findFirst()
                .orElseGet(() -> {
                    Department d = new Department();
                    d.setName("Engineering");
                    return departmentRepository.save(d);
                });

        JobRequest req = new JobRequest();
        req.setTitle("Staff Site Reliability Engineer");
        req.setDepartmentId(dept.getId());
        req.setLocation("Remote");
        req.setEmploymentType(Job.EmploymentType.FULL_TIME);
        req.setSalaryMin(new BigDecimal("2000000"));
        req.setSalaryMax(new BigDecimal("3500000"));
        req.setDescription("Architect high-availability Kubernetes infrastructure and observability pipelines.");
        req.setRequirements("Go, Python, Terraform, Prometheus, Azure, Kubernetes.");

        mockMvc.perform(post("/api/v1/jobs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.title").value("Staff Site Reliability Engineer"))
                .andExpect(jsonPath("$.status").value("DRAFT"));
    }
}
