package com.example.recruitment.service;

import com.example.recruitment.audit.AuditService;
import com.example.recruitment.entity.*;
import com.example.recruitment.notification.NotificationService;
import com.example.recruitment.repository.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import com.example.recruitment.storage.StorageService;

import java.io.IOException;
import java.util.List;
import java.util.Set;

@Service
public class ApplicationService {

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "application/pdf",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
    );
    private static final long MAX_RESUME_BYTES = 5 * 1024 * 1024; // 5 MB

    private final ApplicationRepository applicationRepository;
    private final ApplicationStageHistoryRepository stageHistoryRepository;
    private final JobRepository jobRepository;
    private final ApplicantProfileRepository profileRepository;
    private final ResumeRepository resumeRepository;
    private final UserRepository userRepository;
    private final StorageService storageService;
    private final NotificationService notificationService;
    private final AuditService auditService;

    public ApplicationService(ApplicationRepository applicationRepository,
                               ApplicationStageHistoryRepository stageHistoryRepository,
                               JobRepository jobRepository,
                               ApplicantProfileRepository profileRepository,
                               ResumeRepository resumeRepository,
                               UserRepository userRepository,
                               StorageService storageService,
                               NotificationService notificationService,
                               AuditService auditService) {
        this.applicationRepository = applicationRepository;
        this.stageHistoryRepository = stageHistoryRepository;
        this.jobRepository = jobRepository;
        this.profileRepository = profileRepository;
        this.resumeRepository = resumeRepository;
        this.userRepository = userRepository;
        this.storageService = storageService;
        this.notificationService = notificationService;
        this.auditService = auditService;
    }

    // -----------------------------------------------------------------------
    // APPLICATION SUBMISSION
    // -----------------------------------------------------------------------

    @Transactional
    public Application apply(Long jobId, String applicantUsername,
                              String coverLetter, MultipartFile resumeFile) throws IOException {

        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new IllegalArgumentException("Job not found: " + jobId));

        if (job.getStatus() != Job.Status.PUBLISHED) {
            throw new IllegalStateException("Applications are only accepted for published jobs.");
        }

        ApplicantProfile profile = profileRepository.findByUserId(
                        userRepository.findByUsername(applicantUsername)
                                .orElseThrow().getId())
                .orElseThrow(() -> new IllegalStateException("Complete your profile before applying."));

        if (applicationRepository.existsByJobIdAndApplicantId(jobId, profile.getId())) {
            throw new IllegalStateException("You have already applied to this job.");
        }

        Application app = new Application();
        app.setJob(job);
        app.setApplicant(profile);
        app.setCoverLetter(coverLetter);
        app.setStage(Application.Stage.APPLIED);

        if (resumeFile != null && !resumeFile.isEmpty()) {
            Resume resume = storeResume(resumeFile, profile);
            app.setResume(resume);
        }

        app = applicationRepository.save(app);

        recordStageHistory(app, null, Application.Stage.APPLIED, null, "Application submitted");

        // In-app notification to applicant
        notificationService.send(profile.getUser(),
                "Application submitted",
                "Your application for '" + job.getTitle() + "' has been received.",
                "/applications/" + app.getId());

        auditService.log("APPLICATION_SUBMITTED", "Application", app.getId(),
                "Applicant: " + applicantUsername + " → Job: " + job.getTitle());

        return app;
    }

    // -----------------------------------------------------------------------
    // STAGE TRANSITIONS
    // -----------------------------------------------------------------------

    @Transactional
    public Application changeStage(Long applicationId, Application.Stage newStage,
                                    String actorUsername, String notes) {
        Application app = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new IllegalArgumentException("Application not found: " + applicationId));

        Application.Stage old = app.getStage();
        if (old == newStage) return app;

        app.setStage(newStage);
        applicationRepository.save(app);

        User actor = userRepository.findByUsername(actorUsername).orElse(null);
        recordStageHistory(app, old, newStage, actor, notes);

        // Notify applicant
        notificationService.send(app.getApplicant().getUser(),
                "Application update",
                "Your application for '" + app.getJob().getTitle() + "' is now: " + newStage,
                "/applications/" + app.getId());

        auditService.log("APPLICATION_STAGE_CHANGED", "Application", applicationId,
                old + " → " + newStage + " by " + actorUsername);

        return app;
    }

    private void recordStageHistory(Application app, Application.Stage from,
                                     Application.Stage to, User actor, String notes) {
        ApplicationStageHistory h = new ApplicationStageHistory();
        h.setApplication(app);
        h.setFromStage(from != null ? from.name() : null);
        h.setToStage(to.name());
        h.setChangedBy(actor);
        h.setNotes(notes);
        stageHistoryRepository.save(h);
    }

    // -----------------------------------------------------------------------
    // RESUME UPLOAD / DOWNLOAD
    // -----------------------------------------------------------------------

    @Transactional
    public Resume storeResume(MultipartFile file, ApplicantProfile profile) throws IOException {
        validateResumeFile(file);

        // Deactivate existing resumes
        resumeRepository.findByApplicantIdOrderByUploadedAtDesc(profile.getId())
                .forEach(r -> r.setActive(false));

        String storedPath = storageService.store(file, String.valueOf(profile.getId()));

        Resume resume = new Resume();
        resume.setApplicant(profile);
        resume.setOriginalName(file.getOriginalFilename());
        resume.setStoredPath(storedPath);
        resume.setContentType(file.getContentType());
        resume.setSizeBytes(file.getSize());
        resume.setActive(true);
        return resumeRepository.save(resume);
    }

    private void validateResumeFile(MultipartFile file) {
        if (file.isEmpty()) throw new IllegalArgumentException("Resume file is empty.");
        if (file.getSize() > MAX_RESUME_BYTES)
            throw new IllegalArgumentException("Resume exceeds the 5 MB limit.");
        if (!ALLOWED_CONTENT_TYPES.contains(file.getContentType()))
            throw new IllegalArgumentException("Only PDF and Word documents are accepted.");
    }

    // -----------------------------------------------------------------------
    // QUERIES
    // -----------------------------------------------------------------------

    @Transactional(readOnly = true)
    public Page<Application> findByApplicant(Long applicantId, Pageable pageable) {
        return applicationRepository.findByApplicantId(applicantId, pageable);
    }

    @Transactional(readOnly = true)
    public Page<Application> findByJob(Long jobId, Pageable pageable) {
        return applicationRepository.findByJobId(jobId, pageable);
    }

    @Transactional(readOnly = true)
    public Application findByIdOrThrow(Long id) {
        return applicationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Application not found: " + id));
    }

    @Transactional(readOnly = true)
    public List<ApplicationStageHistory> getHistory(Long applicationId) {
        return stageHistoryRepository.findByApplicationIdOrderByChangedAtAsc(applicationId);
    }

    @Transactional(readOnly = true)
    public long countByStage(Application.Stage stage) {
        return applicationRepository.countByStage(stage);
    }

    @Transactional(readOnly = true)
    public List<Object[]> countByStageGrouped() {
        return applicationRepository.countByStageGrouped();
    }

    @Transactional(readOnly = true)
    public List<Application> getRecentForRecruiter(Long recruiterId) {
        return applicationRepository.findRecentByRecruiter(recruiterId,
                PageRequest.of(0, 10));
    }
}
