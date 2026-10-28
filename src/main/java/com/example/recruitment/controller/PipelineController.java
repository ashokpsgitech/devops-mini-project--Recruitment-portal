package com.example.recruitment.controller;

import com.example.recruitment.entity.Application;
import com.example.recruitment.entity.Job;
import com.example.recruitment.repository.ApplicationRepository;
import com.example.recruitment.service.ApplicationService;
import com.example.recruitment.service.JobService;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/recruiter/pipeline")
public class PipelineController {

    private final ApplicationRepository applicationRepository;
    private final ApplicationService applicationService;
    private final JobService jobService;

    public PipelineController(ApplicationRepository applicationRepository,
                              ApplicationService applicationService,
                              JobService jobService) {
        this.applicationRepository = applicationRepository;
        this.applicationService = applicationService;
        this.jobService = jobService;
    }

    @GetMapping
    public String pipelineBoard(@RequestParam(required = false) Long jobId, Model model) {
        List<Job> allJobs = jobService.findAll(Pageable.unpaged()).getContent();
        model.addAttribute("jobs", allJobs);
        model.addAttribute("selectedJobId", jobId);

        List<Application> applications;
        if (jobId != null) {
            applications = applicationRepository.findByJobId(jobId, Pageable.unpaged()).getContent();
        } else {
            applications = applicationRepository.findAll();
        }

        Map<Application.Stage, List<Application>> groupedByStage = Arrays.stream(Application.Stage.values())
                .collect(Collectors.toMap(
                        stage -> stage,
                        stage -> applications.stream().filter(a -> a.getStage() == stage).toList()
                ));

        model.addAttribute("pipeline", groupedByStage);
        model.addAttribute("stages", Application.Stage.values());
        return "recruiter/pipeline";
    }

    @PostMapping("/{applicationId}/move")
    public String moveCandidate(@PathVariable Long applicationId,
                                @RequestParam Application.Stage newStage,
                                @RequestParam(required = false) Long currentJobId,
                                @RequestParam(required = false) String notes,
                                Authentication auth,
                                RedirectAttributes redirectAttributes) {
        applicationService.changeStage(applicationId, newStage, auth.getName(), notes);
        redirectAttributes.addFlashAttribute("success", "Candidate moved to stage: " + newStage);

        if (currentJobId != null) {
            return "redirect:/recruiter/pipeline?jobId=" + currentJobId;
        }
        return "redirect:/recruiter/pipeline";
    }
}
