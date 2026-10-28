package com.example.recruitment.controller;

import com.example.recruitment.entity.*;
import com.example.recruitment.repository.ApplicantProfileRepository;
import com.example.recruitment.repository.ResumeRepository;
import com.example.recruitment.repository.UserRepository;
import com.example.recruitment.service.ApplicationService;
import com.example.recruitment.service.InterviewService;
import com.example.recruitment.service.JobService;
import com.example.recruitment.service.OfferService;
import com.example.recruitment.storage.StorageService;
import org.springframework.core.io.InputStreamResource;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;

@Controller
@RequestMapping("/applications")
public class ApplicationController {

    private final ApplicationService applicationService;
    private final JobService jobService;
    private final InterviewService interviewService;
    private final OfferService offerService;
    private final UserRepository userRepository;
    private final ApplicantProfileRepository profileRepository;
    private final ResumeRepository resumeRepository;
    private final StorageService storageService;

    public ApplicationController(ApplicationService applicationService,
                                   JobService jobService,
                                   InterviewService interviewService,
                                   OfferService offerService,
                                   UserRepository userRepository,
                                   ApplicantProfileRepository profileRepository,
                                   ResumeRepository resumeRepository,
                                   StorageService storageService) {
        this.applicationService = applicationService;
        this.jobService = jobService;
        this.interviewService = interviewService;
        this.offerService = offerService;
        this.userRepository = userRepository;
        this.profileRepository = profileRepository;
        this.resumeRepository = resumeRepository;
        this.storageService = storageService;
    }

    // -----------------------------------------------------------------------
    // APPLICANT — APPLY TO JOB
    // -----------------------------------------------------------------------

    @GetMapping("/apply/{jobId}")
    public String applyForm(@PathVariable Long jobId, Authentication auth, Model model) {
        model.addAttribute("job", jobService.getPublishedJobOrThrow(jobId));
        return "applications/apply";
    }

    @PostMapping("/apply/{jobId}")
    public String submitApplication(@PathVariable Long jobId,
                                     @RequestParam(required = false) String coverLetter,
                                     @RequestParam(required = false) MultipartFile resumeFile,
                                     Authentication auth,
                                     RedirectAttributes redirectAttributes) {
        try {
            applicationService.apply(jobId, auth.getName(), coverLetter, resumeFile);
            redirectAttributes.addFlashAttribute("success",
                    "Your application has been submitted successfully!");
            return "redirect:/dashboard";
        } catch (IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
            return "redirect:/jobs/" + jobId;
        } catch (IOException ex) {
            redirectAttributes.addFlashAttribute("error",
                    "Resume upload failed. Please try again.");
            return "redirect:/applications/apply/" + jobId;
        }
    }

    // -----------------------------------------------------------------------
    // APPLICANT — VIEW OWN APPLICATIONS
    // -----------------------------------------------------------------------

    @GetMapping
    public String myApplications(Authentication auth,
                                   @RequestParam(defaultValue = "0") int page,
                                   Model model) {
        User user = userRepository.findByUsername(auth.getName()).orElseThrow();
        ApplicantProfile profile = profileRepository.findByUserId(user.getId()).orElseThrow();
        var pageable = PageRequest.of(page, 10, Sort.by("appliedAt").descending());
        model.addAttribute("applications",
                applicationService.findByApplicant(profile.getId(), pageable));
        return "applications/my-applications";
    }

    @GetMapping("/{id}")
    public String applicationDetail(@PathVariable Long id, Authentication auth, Model model) {
        Application app = applicationService.findByIdOrThrow(id);
        enforceApplicantOwnership(app, auth);

        model.addAttribute("application", app);
        model.addAttribute("history", applicationService.getHistory(id));
        model.addAttribute("interviews", interviewService.findByApplication(id));

        offerService.countPending(); // warm-up; actual offer for this app:
        com.example.recruitment.repository.OfferRepository offerRepo =
                applicationService.findByIdOrThrow(id) != null ? null : null;
        return "applications/detail";
    }

    // -----------------------------------------------------------------------
    // RECRUITER — VIEW APPLICATIONS FOR A JOB
    // -----------------------------------------------------------------------

    @GetMapping("/job/{jobId}")
    public String applicationsForJob(@PathVariable Long jobId,
                                      @RequestParam(defaultValue = "0") int page,
                                      @RequestParam(required = false) Application.Stage stage,
                                      Model model) {
        var pageable = PageRequest.of(page, 20, Sort.by("appliedAt").descending());
        model.addAttribute("job", jobService.findByIdOrThrow(jobId));
        if (stage != null) {
            model.addAttribute("applications",
                    applicationService.findByJob(jobId, pageable));
            model.addAttribute("stageFilter", stage);
        } else {
            model.addAttribute("applications",
                    applicationService.findByJob(jobId, pageable));
        }
        model.addAttribute("stages", Application.Stage.values());
        return "applications/job-applications";
    }

    // -----------------------------------------------------------------------
    // RECRUITER — CHANGE STAGE
    // -----------------------------------------------------------------------

    @PostMapping("/{id}/stage")
    public String changeStage(@PathVariable Long id,
                               @RequestParam Application.Stage stage,
                               @RequestParam(required = false) String notes,
                               Authentication auth,
                               RedirectAttributes redirectAttributes) {
        applicationService.changeStage(id, stage, auth.getName(), notes);
        redirectAttributes.addFlashAttribute("success",
                "Application moved to " + stage + ".");
        return "redirect:/applications/" + id;
    }

    // -----------------------------------------------------------------------
    // RESUME DOWNLOAD (Authorization: applicant owner or recruiter)
    // -----------------------------------------------------------------------

    @GetMapping("/resume/{resumeId}/download")
    public ResponseEntity<InputStreamResource> downloadResume(
            @PathVariable Long resumeId, Authentication auth) throws IOException {

        Resume resume = resumeRepository.findById(resumeId)
                .orElseThrow(() -> new IllegalArgumentException("Resume not found."));

        // Only the resume owner or a recruiter/admin may download
        User requester = userRepository.findByUsername(auth.getName()).orElseThrow();
        boolean isOwner = profileRepository.findByUserId(requester.getId())
                .map(p -> p.getId().equals(resume.getApplicant().getId()))
                .orElse(false);
        boolean isPrivileged = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_RECRUITER")
                        || a.getAuthority().equals("ROLE_ADMIN")
                        || a.getAuthority().equals("ROLE_HIRING_MANAGER"));

        if (!isOwner && !isPrivileged) {
            throw new AccessDeniedException("You are not authorized to download this resume.");
        }

        var stream = storageService.load(resume.getStoredPath());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + resume.getOriginalName() + "\"")
                .contentType(MediaType.parseMediaType(resume.getContentType()))
                .body(new InputStreamResource(stream));
    }

    // -----------------------------------------------------------------------
    // HELPERS
    // -----------------------------------------------------------------------

    private void enforceApplicantOwnership(Application app, Authentication auth) {
        boolean isOwner = app.getApplicant().getUser().getUsername().equals(auth.getName());
        boolean isPrivileged = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_RECRUITER")
                        || a.getAuthority().equals("ROLE_ADMIN")
                        || a.getAuthority().equals("ROLE_HIRING_MANAGER"));
        if (!isOwner && !isPrivileged) {
            throw new AccessDeniedException("Access denied.");
        }
    }
}
