package com.example.recruitment.service;

import com.example.recruitment.audit.AuditService;
import com.example.recruitment.entity.*;
import com.example.recruitment.notification.NotificationService;
import com.example.recruitment.repository.*;
import com.example.recruitment.storage.StorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ApplicationServiceTest {

    @Mock private ApplicationRepository applicationRepository;
    @Mock private ApplicationStageHistoryRepository historyRepository;
    @Mock private JobRepository jobRepository;
    @Mock private UserRepository userRepository;
    @Mock private ApplicantProfileRepository profileRepository;
    @Mock private ResumeRepository resumeRepository;
    @Mock private StorageService storageService;
    @Mock private NotificationService notificationService;
    @Mock private AuditService auditService;

    private ApplicationService applicationService;

    @BeforeEach
    void setUp() {
        applicationService = new ApplicationService(
                applicationRepository, historyRepository, jobRepository,
                profileRepository, resumeRepository, userRepository,
                storageService, notificationService, auditService);
    }

    @Test
    @DisplayName("Should submit application successfully with resume")
    void testApplySuccess() throws Exception {
        Long jobId = 1L;
        String username = "candidate1";

        Job job = new Job();
        job.setId(jobId);
        job.setTitle("Cloud Architect");
        job.setStatus(Job.Status.PUBLISHED);

        User user = new User();
        user.setId(10L);
        user.setUsername(username);
        user.setEmail("cand@test.com");

        ApplicantProfile profile = new ApplicantProfile();
        profile.setId(20L);
        profile.setUser(user);
        profile.setFirstName("John");
        profile.setLastName("Doe");

        when(jobRepository.findById(jobId)).thenReturn(Optional.of(job));
        when(userRepository.findByUsername(username)).thenReturn(Optional.of(user));
        when(profileRepository.findByUserId(10L)).thenReturn(Optional.of(profile));
        when(applicationRepository.existsByJobIdAndApplicantId(jobId, 20L)).thenReturn(false);

        MockMultipartFile resumeFile = new MockMultipartFile(
                "resumeFile", "resume.pdf", "application/pdf", "dummy pdf content".getBytes());
        when(storageService.store(any(), any())).thenReturn("20/uuid.pdf");

        Resume resume = new Resume();
        resume.setId(100L);
        when(resumeRepository.save(any())).thenReturn(resume);

        when(applicationRepository.save(any(Application.class))).thenAnswer(inv -> {
            Application app = inv.getArgument(0);
            app.setId(500L);
            return app;
        });

        Application result = applicationService.apply(jobId, username, "Cover letter notes", resumeFile);

        assertNotNull(result);
        assertEquals(Application.Stage.APPLIED, result.getStage());
        verify(historyRepository, times(1)).save(any(ApplicationStageHistory.class));
        verify(auditService, times(1)).log(eq("APPLICATION_SUBMITTED"), eq("Application"), eq(500L), any());
    }

    @Test
    @DisplayName("Should reject duplicate application for same job")
    void testApplyDuplicateThrowsException() {
        Long jobId = 1L;
        String username = "candidate1";

        Job job = new Job();
        job.setId(jobId);
        job.setStatus(Job.Status.PUBLISHED);

        User user = new User();
        user.setId(10L);

        ApplicantProfile profile = new ApplicantProfile();
        profile.setId(20L);

        when(jobRepository.findById(jobId)).thenReturn(Optional.of(job));
        when(userRepository.findByUsername(username)).thenReturn(Optional.of(user));
        when(profileRepository.findByUserId(10L)).thenReturn(Optional.of(profile));
        when(applicationRepository.existsByJobIdAndApplicantId(jobId, 20L)).thenReturn(true);

        assertThrows(IllegalStateException.class, () ->
                applicationService.apply(jobId, username, null, null));
    }

    @Test
    @DisplayName("Should successfully change stage and record audit history")
    void testChangeStageSuccess() {
        Long appId = 500L;
        Application app = new Application();
        app.setId(appId);
        app.setStage(Application.Stage.APPLIED);

        Job job = new Job();
        job.setTitle("Staff Engineer");
        app.setJob(job);

        ApplicantProfile profile = new ApplicantProfile();
        User candidateUser = new User();
        candidateUser.setEmail("cand@test.com");
        profile.setUser(candidateUser);
        app.setApplicant(profile);

        when(applicationRepository.findById(appId)).thenReturn(Optional.of(app));
        when(applicationRepository.save(any(Application.class))).thenReturn(app);

        applicationService.changeStage(appId, Application.Stage.SHORTLISTED, "recruiter1", "Strong system design skills");

        assertEquals(Application.Stage.SHORTLISTED, app.getStage());
        verify(historyRepository, times(1)).save(any(ApplicationStageHistory.class));
        verify(notificationService, times(1)).send(eq(candidateUser), any(), any(), any());
        verify(auditService, times(1)).log(eq("APPLICATION_STAGE_CHANGED"), eq("Application"), eq(appId), any());
    }
}
