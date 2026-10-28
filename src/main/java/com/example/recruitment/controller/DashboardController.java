package com.example.recruitment.controller;

import com.example.recruitment.entity.Application;
import com.example.recruitment.entity.Job;
import com.example.recruitment.notification.NotificationService;
import com.example.recruitment.repository.ApplicantProfileRepository;
import com.example.recruitment.repository.UserRepository;
import com.example.recruitment.service.ApplicationService;
import com.example.recruitment.service.InterviewService;
import com.example.recruitment.service.JobService;
import com.example.recruitment.service.OfferService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DashboardController {

    private final JobService jobService;
    private final ApplicationService applicationService;
    private final InterviewService interviewService;
    private final OfferService offerService;
    private final NotificationService notificationService;
    private final UserRepository userRepository;
    private final ApplicantProfileRepository profileRepository;

    public DashboardController(JobService jobService,
                                ApplicationService applicationService,
                                InterviewService interviewService,
                                OfferService offerService,
                                NotificationService notificationService,
                                UserRepository userRepository,
                                ApplicantProfileRepository profileRepository) {
        this.jobService = jobService;
        this.applicationService = applicationService;
        this.interviewService = interviewService;
        this.offerService = offerService;
        this.notificationService = notificationService;
        this.userRepository = userRepository;
        this.profileRepository = profileRepository;
    }

    @GetMapping({"/", "/dashboard"})
    public String dashboard(Authentication auth, Model model) {
        if (auth == null || !auth.isAuthenticated()) {
            return "redirect:/login";
        }

        String username = auth.getName();
        var user = userRepository.findByUsername(username).orElseThrow();
        model.addAttribute("username", username);
        model.addAttribute("unreadNotifications", notificationService.countUnread(user.getId()));

        if (auth.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_ADMIN"))) {
            return buildAdminDashboard(model, user.getId());
        } else if (auth.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_RECRUITER"))) {
            return buildRecruiterDashboard(model, user.getId());
        } else if (auth.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_HIRING_MANAGER"))) {
            return buildHiringManagerDashboard(model, user.getId());
        } else {
            return buildApplicantDashboard(model, user.getId());
        }
    }

    private String buildAdminDashboard(Model model, Long userId) {
        model.addAttribute("totalJobs", jobService.countByStatus(Job.Status.PUBLISHED));
        model.addAttribute("totalApplications", applicationService.countByStage(Application.Stage.APPLIED));
        model.addAttribute("totalHired", applicationService.countByStage(Application.Stage.HIRED));
        model.addAttribute("stageCounts", applicationService.countByStageGrouped());
        model.addAttribute("recentJobs", jobService.getRecentPublished());
        return "dashboard/admin";
    }

    private String buildRecruiterDashboard(Model model, Long userId) {
        model.addAttribute("openJobs", jobService.countByStatus(Job.Status.PUBLISHED));
        model.addAttribute("applied", applicationService.countByStage(Application.Stage.APPLIED));
        model.addAttribute("shortlisted", applicationService.countByStage(Application.Stage.SHORTLISTED));
        model.addAttribute("interviews", applicationService.countByStage(Application.Stage.INTERVIEW));
        model.addAttribute("pendingOffers", offerService.countPending());
        model.addAttribute("recentActivity", applicationService.getRecentForRecruiter(userId));
        model.addAttribute("upcomingInterviews", interviewService.findUpcomingForInterviewer(userId));
        return "dashboard/recruiter";
    }

    private String buildHiringManagerDashboard(Model model, Long userId) {
        model.addAttribute("upcomingInterviews", interviewService.findUpcomingForInterviewer(userId));
        model.addAttribute("pendingOffers", offerService.countPending());
        return "dashboard/hiring-manager";
    }

    private String buildApplicantDashboard(Model model, Long userId) {
        profileRepository.findByUserId(userId).ifPresent(profile -> {
            model.addAttribute("applications",
                    applicationService.findByApplicant(profile.getId(),
                            org.springframework.data.domain.PageRequest.of(0, 5)));
            model.addAttribute("upcomingInterviews",
                    interviewService.findUpcomingForApplicant(userId));
        });
        return "dashboard/applicant";
    }
}
