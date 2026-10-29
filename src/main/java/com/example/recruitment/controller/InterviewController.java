package com.example.recruitment.controller;

import com.example.recruitment.entity.*;
import com.example.recruitment.repository.InterviewRepository;
import com.example.recruitment.repository.UserRepository;
import com.example.recruitment.service.ApplicationService;
import com.example.recruitment.service.InterviewService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Controller
@RequestMapping("/interviews")
public class InterviewController {

    private final InterviewService interviewService;
    private final ApplicationService applicationService;
    private final InterviewRepository interviewRepository;
    private final UserRepository userRepository;

    public InterviewController(InterviewService interviewService,
                               ApplicationService applicationService,
                               InterviewRepository interviewRepository,
                               UserRepository userRepository) {
        this.interviewService = interviewService;
        this.applicationService = applicationService;
        this.interviewRepository = interviewRepository;
        this.userRepository = userRepository;
    }

    @GetMapping
    public String listInterviews(Authentication auth, Model model) {
        User user = userRepository.findByUsername(auth.getName()).orElseThrow();
        boolean isRecruiterOrAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_RECRUITER") || a.getAuthority().equals("ROLE_ADMIN"));
        boolean isInterviewer = auth.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_HIRING_MANAGER"));

        if (isRecruiterOrAdmin) {
            model.addAttribute("interviews", interviewRepository.findAll());
        } else if (isInterviewer) {
            model.addAttribute("interviews", interviewService.findUpcomingForInterviewer(user.getId()));
        } else {
            model.addAttribute("interviews", interviewService.findUpcomingForApplicant(user.getId()));
        }
        return "interviews/list";
    }

    @GetMapping("/schedule/{applicationId}")
    public String scheduleForm(@PathVariable Long applicationId, Model model) {
        Application application = applicationService.findByIdOrThrow(applicationId);
        model.addAttribute("application", application);
        model.addAttribute("app", application);
        model.addAttribute("interviewTypes", Interview.InterviewType.values());
        model.addAttribute("interviewers", userRepository.findAll());
        return "interviews/schedule";
    }

    @PostMapping("/schedule/{applicationId}")
    public String scheduleInterview(@PathVariable Long applicationId,
                                    @RequestParam Long interviewerId,
                                    @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime scheduledTime,
                                    @RequestParam(defaultValue = "60") int durationMinutes,
                                    @RequestParam Interview.InterviewType interviewType,
                                    @RequestParam String locationOrUrl,
                                    @RequestParam(required = false) String notes,
                                    RedirectAttributes redirectAttributes) {
        Instant scheduledAt = scheduledTime.toInstant(ZoneOffset.UTC);
        Interview interview = interviewService.schedule(
                applicationId, scheduledAt, durationMinutes, locationOrUrl, interviewType, interviewerId, notes);
        redirectAttributes.addFlashAttribute("success",
                "Interview scheduled successfully with " + (interview.getInterviewer() != null ? interview.getInterviewer().getFullName() : "assigned interviewer") + ".");
        return "redirect:/applications/" + applicationId;
    }

    @GetMapping("/{id}")
    public String interviewDetail(@PathVariable Long id, Authentication auth, Model model) {
        Interview interview = interviewService.findByIdOrThrow(id);
        model.addAttribute("interview", interview);
        model.addAttribute("feedback", interviewService.getFeedbackForInterview(id).orElse(null));
        return "interviews/detail";
    }

    @GetMapping("/{id}/feedback")
    public String feedbackForm(@PathVariable Long id, Authentication auth, Model model) {
        Interview interview = interviewService.findByIdOrThrow(id);
        User current = userRepository.findByUsername(auth.getName()).orElseThrow();
        boolean isInterviewer = interview.getInterviewer() != null && interview.getInterviewer().getId().equals(current.getId());
        boolean isPrivileged = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_RECRUITER") || a.getAuthority().equals("ROLE_ADMIN"));

        if (!isInterviewer && !isPrivileged) {
            throw new AccessDeniedException("Only the assigned interviewer or recruiter may submit feedback.");
        }

        model.addAttribute("interview", interview);
        return "interviews/feedback-form";
    }

    @PostMapping("/{id}/feedback")
    public String submitFeedback(@PathVariable Long id,
                                 @RequestParam short rating,
                                 @RequestParam String recommendation,
                                 @RequestParam String technicalNotes,
                                 @RequestParam(required = false) String culturalFitNotes,
                                 Authentication auth,
                                 RedirectAttributes redirectAttributes) {
        interviewService.submitFeedback(id, rating, recommendation, technicalNotes, culturalFitNotes, technicalNotes, auth.getName());
        redirectAttributes.addFlashAttribute("success", "Interview feedback recorded successfully.");
        return "redirect:/interviews/" + id;
    }

    @PostMapping("/{id}/cancel")
    public String cancelInterview(@PathVariable Long id,
                                  @RequestParam(required = false) String reason,
                                  RedirectAttributes redirectAttributes) {
        interviewService.cancel(id, reason);
        redirectAttributes.addFlashAttribute("success", "Interview cancelled.");
        return "redirect:/interviews";
    }
}
