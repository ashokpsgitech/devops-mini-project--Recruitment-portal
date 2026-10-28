package com.example.recruitment.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class SecurityAuthorizationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("Public user should be allowed to view /jobs")
    void publicAccessToJobs() throws Exception {
        mockMvc.perform(get("/jobs"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Unauthenticated user accessing protected recruiter pipeline should be rejected with 401")
    void unauthenticatedAccessRejected() throws Exception {
        mockMvc.perform(get("/recruiter/pipeline"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "APPLICANT")
    @DisplayName("Applicant role should be denied (403) from accessing recruiter pipeline")
    void applicantDeniedFromRecruiterPipeline() throws Exception {
        mockMvc.perform(get("/recruiter/pipeline"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "RECRUITER")
    @DisplayName("Recruiter role should be allowed to access recruiter pipeline")
    void recruiterAllowedAccessToPipeline() throws Exception {
        mockMvc.perform(get("/recruiter/pipeline"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Admin seed password should match BCrypt hash")
    void adminSeedPasswordMatchesBcrypt() {
        var encoder = new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder(12);
        String hash = "$2a$12$0VMe5b0QdxMMI2dFHASgVuKQAXvcJCHKsB2frhu56XkB/gLGnfYz6";
        org.junit.jupiter.api.Assertions.assertTrue(encoder.matches("Admin@123!", hash));
    }
}
