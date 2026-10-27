package com.example.recruitment.job;

import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class JobController {

    private final JobRepository jobRepository;

    public JobController(JobRepository jobRepository) {
        this.jobRepository = jobRepository;
    }

    @GetMapping("/")
    public String home() {
        return "redirect:/jobs";
    }

    @GetMapping("/jobs")
    public String listJobs(Model model) {
        model.addAttribute("jobs", jobRepository.findAll());
        return "jobs";
    }

    @GetMapping("/jobs/new")
    public String newJob(Model model) {
        model.addAttribute("job", new Job());
        return "job-form";
    }

    @PostMapping("/jobs")
    public String createJob(
            @Valid @ModelAttribute("job") Job job,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            return "job-form";
        }

        job.setStatus("OPEN");
        jobRepository.save(job);

        redirectAttributes.addFlashAttribute(
                "message", "Job published successfully!");

        return "redirect:/jobs";
    }
}