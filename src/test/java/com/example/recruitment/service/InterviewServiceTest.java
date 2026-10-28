package com.example.recruitment.service;

import com.example.recruitment.audit.AuditService;
import com.example.recruitment.entity.*;
import com.example.recruitment.notification.NotificationService;
import com.example.recruitment.repository.ApplicationRepository;
import com.example.recruitment.repository.InterviewFeedbackRepository;
import com.example.recruitment.repository.InterviewRepository;
import com.example.recruitment.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InterviewServiceTest {

    @Mock private InterviewRepository interviewRepository;
    @Mock private InterviewFeedbackRepository feedbackRepository;
    @Mock private ApplicationRepository applicationRepository;
    @Mock private UserRepository userRepository;
    @Mock private NotificationService notificationService;
    @Mock private AuditService auditService;

    private InterviewService interviewService;

    @BeforeEach
    void setUp() {
        interviewService = new InterviewService(
                interviewRepository, feedbackRepository, applicationRepository,
                userRepository, notificationService, auditService);
    }

    @Test
    @DisplayName("Should schedule interview and send candidate notification")
    void testScheduleInterviewSuccess() {
        Long appId = 1L;
        Application app = new Application();
        app.setId(appId);
        Job job = new Job();
        job.setTitle("Senior Architect");
        app.setJob(job);

        User candidateUser = new User();
        ApplicantProfile profile = new ApplicantProfile();
        profile.setUser(candidateUser);
        app.setApplicant(profile);

        when(applicationRepository.findById(appId)).thenReturn(Optional.of(app));
        when(interviewRepository.save(any(Interview.class))).thenAnswer(inv -> {
            Interview i = inv.getArgument(0);
            i.setId(50L);
            return i;
        });

        Interview result = interviewService.schedule(
                appId, Instant.now().plusSeconds(86400), 45,
                "https://meet.google.com/test", Interview.InterviewType.TECHNICAL, null);

        assertNotNull(result);
        assertEquals(Interview.Status.SCHEDULED, result.getStatus());
        verify(notificationService, times(1)).send(eq(candidateUser), any(), any(), any());
        verify(auditService, times(1)).log(eq("INTERVIEW_SCHEDULED"), eq("Interview"), eq(50L), any());
    }

    @Test
    @DisplayName("Should submit feedback and mark interview as COMPLETED")
    void testSubmitFeedbackSuccess() {
        Long interviewId = 50L;
        Interview interview = new Interview();
        interview.setId(interviewId);
        interview.setStatus(Interview.Status.SCHEDULED);

        when(interviewRepository.findById(interviewId)).thenReturn(Optional.of(interview));
        when(feedbackRepository.findByInterviewId(interviewId)).thenReturn(Optional.empty());
        when(feedbackRepository.save(any(InterviewFeedback.class))).thenAnswer(inv -> inv.getArgument(0));

        InterviewFeedback feedback = interviewService.submitFeedback(
                interviewId, (short) 5, "STRONG_YES", "Great depth in Java and Distributed Systems",
                "Excellent communicator", "Clear hire recommendation", "interviewer1");

        assertNotNull(feedback);
        assertEquals(Interview.Status.COMPLETED, interview.getStatus());
        assertEquals("STRONG_YES", feedback.getRecommendation());
        verify(auditService, times(1)).log(eq("INTERVIEW_FEEDBACK_SUBMITTED"), eq("Interview"), eq(interviewId), any());
    }
}
