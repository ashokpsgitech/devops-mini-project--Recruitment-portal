package com.example.recruitment.service;

import com.example.recruitment.audit.AuditService;
import com.example.recruitment.entity.Application;
import com.example.recruitment.entity.Interview;
import com.example.recruitment.entity.InterviewFeedback;
import com.example.recruitment.entity.User;
import com.example.recruitment.notification.NotificationService;
import com.example.recruitment.repository.ApplicationRepository;
import com.example.recruitment.repository.InterviewFeedbackRepository;
import com.example.recruitment.repository.InterviewRepository;
import com.example.recruitment.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
public class InterviewService {

    private final InterviewRepository interviewRepository;
    private final InterviewFeedbackRepository feedbackRepository;
    private final ApplicationRepository applicationRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final AuditService auditService;

    public InterviewService(InterviewRepository interviewRepository,
                             InterviewFeedbackRepository feedbackRepository,
                             ApplicationRepository applicationRepository,
                             UserRepository userRepository,
                             NotificationService notificationService,
                             AuditService auditService) {
        this.interviewRepository = interviewRepository;
        this.feedbackRepository = feedbackRepository;
        this.applicationRepository = applicationRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
        this.auditService = auditService;
    }

    @Transactional
    public Interview schedule(Long applicationId, Instant scheduledAt, Integer durationMins,
                               String location, Interview.InterviewType type, Long interviewerId) {
        return schedule(applicationId, scheduledAt, durationMins, location, type, interviewerId, null);
    }

    @Transactional
    public Interview schedule(Long applicationId, Instant scheduledAt, Integer durationMins,
                               String location, Interview.InterviewType type, Long interviewerId,
                               String notes) {
        Application app = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new IllegalArgumentException("Application not found: " + applicationId));

        Interview interview = new Interview();
        interview.setApplication(app);
        interview.setScheduledAt(scheduledAt);
        interview.setDurationMins(durationMins != null ? durationMins : 60);
        interview.setLocation(location);
        interview.setInterviewType(type);
        interview.setStatus(Interview.Status.SCHEDULED);
        interview.setNotes(notes);

        if (interviewerId != null) {
            userRepository.findById(interviewerId).ifPresent(interview::setInterviewer);
        }

        // Auto-advance application stage to INTERVIEW if currently in APPLIED or SHORTLISTED
        if (app.getStage() == Application.Stage.APPLIED || app.getStage() == Application.Stage.SHORTLISTED) {
            app.setStage(Application.Stage.INTERVIEW);
            applicationRepository.save(app);
        }

        Interview saved = interviewRepository.save(interview);

        // Notify applicant
        notificationService.send(app.getApplicant().getUser(),
                "Interview scheduled",
                "An interview for '" + app.getJob().getTitle() + "' has been scheduled.",
                "/applications/" + app.getId());

        auditService.log("INTERVIEW_SCHEDULED", "Interview", saved.getId(),
                "Application #" + applicationId);
        return saved;
    }

    @Transactional
    public InterviewFeedback submitFeedback(Long interviewId, Short rating,
                                             String recommendation, String strengths,
                                             String concerns, String summary,
                                             String submitterUsername) {
        Interview interview = interviewRepository.findById(interviewId)
                .orElseThrow(() -> new IllegalArgumentException("Interview not found: " + interviewId));

        InterviewFeedback feedback = feedbackRepository.findByInterviewId(interviewId)
                .orElse(new InterviewFeedback());
        feedback.setInterview(interview);
        feedback.setRating(rating);
        feedback.setRecommendation(recommendation);
        feedback.setStrengths(strengths);
        feedback.setConcerns(concerns);
        feedback.setSummary(summary);
        userRepository.findByUsername(submitterUsername).ifPresent(feedback::setSubmittedBy);
        interview.setStatus(Interview.Status.COMPLETED);

        auditService.log("INTERVIEW_FEEDBACK_SUBMITTED", "Interview", interviewId,
                "By: " + submitterUsername);
        return feedbackRepository.save(feedback);
    }

    @Transactional(readOnly = true)
    public List<Interview> findUpcomingForInterviewer(Long userId) {
        return interviewRepository.findUpcomingForInterviewer(userId, Instant.now());
    }

    @Transactional(readOnly = true)
    public List<Interview> findUpcomingForApplicant(Long userId) {
        return interviewRepository.findUpcomingForApplicant(userId, Instant.now());
    }

    @Transactional(readOnly = true)
    public Interview findByIdOrThrow(Long id) {
        return interviewRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Interview not found: " + id));
    }

    @Transactional
    public void cancel(Long interviewId, String reason) {
        Interview interview = findByIdOrThrow(interviewId);
        interview.setStatus(Interview.Status.CANCELLED);
        if (reason != null && !reason.isBlank()) {
            interview.setNotes(interview.getNotes() != null ? interview.getNotes() + "\nCancellation reason: " + reason : "Cancellation reason: " + reason);
        }
        interviewRepository.save(interview);
        auditService.log("INTERVIEW_CANCELLED", "Interview", interviewId, reason != null ? reason : "Cancelled");
    }

    @Transactional(readOnly = true)
    public java.util.Optional<InterviewFeedback> getFeedbackForInterview(Long interviewId) {
        return feedbackRepository.findByInterviewId(interviewId);
    }

    @Transactional(readOnly = true)
    public List<Interview> findByApplication(Long applicationId) {
        return interviewRepository.findByApplicationId(applicationId);
    }
}
