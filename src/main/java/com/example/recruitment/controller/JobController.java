package com.example.recruitment.controller;

import com.example.recruitment.dto.JobRequest;
import com.example.recruitment.entity.Job;
import com.example.recruitment.service.JobService;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class JobController {

    private final JobService jobService;

    public JobController(JobService jobService) {
        this.jobService = jobService;
    }

    // -----------------------------------------------------------------------
    // PUBLIC JOB LISTINGS
    // -----------------------------------------------------------------------

    @GetMapping("/jobs")
    public String listPublicJobs(@RequestParam(required = false) String title,
                                  @RequestParam(required = false) String location,
                                  @RequestParam(defaultValue = "0") int page,
                                  @RequestParam(defaultValue = "12") int size,
                                  Model model) {
        var pageable = PageRequest.of(page, size, Sort.by("publishedAt").descending());
        model.addAttribute("jobs", jobService.searchPublished(title, location, pageable));
        model.addAttribute("title", title);
        model.addAttribute("location", location);
        return "jobs/list";
    }

    @GetMapping("/jobs/{id}")
    public String jobDetail(@PathVariable Long id, Model model) {
        model.addAttribute("job", jobService.getPublishedJobOrThrow(id));
        return "jobs/detail";
    }

    // -----------------------------------------------------------------------
    // RECRUITER JOB MANAGEMENT
    // -----------------------------------------------------------------------

    @GetMapping("/recruiter/jobs")
    public String manageJobs(@RequestParam(defaultValue = "0") int page,
                              @RequestParam(defaultValue = "20") int size,
                              Model model) {
        var pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        model.addAttribute("jobs", jobService.findAll(pageable));
        return "jobs/manage";
    }

    @GetMapping("/jobs/new")
    public String newJobForm(Model model) {
        model.addAttribute("job", new JobRequest());
        model.addAttribute("departments", jobService.getAllDepartments());
        model.addAttribute("employmentTypes", Job.EmploymentType.values());
        return "jobs/form";
    }

    @PostMapping("/jobs")
    public String createJob(@Valid @ModelAttribute("job") JobRequest request,
                             BindingResult bindingResult,
                             Authentication auth,
                             RedirectAttributes redirectAttributes,
                             Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("departments", jobService.getAllDepartments());
            model.addAttribute("employmentTypes", Job.EmploymentType.values());
            return "jobs/form";
        }

        Job job = new Job();
        job.setTitle(request.getTitle());
        job.setLocation(request.getLocation());
        job.setEmploymentType(request.getEmploymentType());
        job.setSalaryMin(request.getSalaryMin());
        job.setSalaryMax(request.getSalaryMax());
        job.setDescription(request.getDescription());
        job.setRequirements(request.getRequirements());
        job.setClosingDate(request.getClosingDate());

        Job saved = jobService.create(job, request.getDepartmentId(), auth.getName());
        redirectAttributes.addFlashAttribute("success", "Job draft saved: " + saved.getTitle());
        return "redirect:/recruiter/jobs";
    }

    @GetMapping("/jobs/{id}/edit")
    public String editJobForm(@PathVariable Long id, Model model) {
        Job job = jobService.findByIdOrThrow(id);
        JobRequest req = new JobRequest();
        req.setTitle(job.getTitle());
        req.setLocation(job.getLocation());
        req.setEmploymentType(job.getEmploymentType());
        req.setSalaryMin(job.getSalaryMin());
        req.setSalaryMax(job.getSalaryMax());
        req.setDescription(job.getDescription());
        req.setRequirements(job.getRequirements());
        req.setClosingDate(job.getClosingDate());
        req.setDepartmentId(job.getDepartment() != null ? job.getDepartment().getId() : null);
        model.addAttribute("job", req);
        model.addAttribute("jobId", id);
        model.addAttribute("departments", jobService.getAllDepartments());
        model.addAttribute("employmentTypes", Job.EmploymentType.values());
        return "jobs/form";
    }

    @PostMapping("/jobs/{id}")
    public String updateJob(@PathVariable Long id,
                             @Valid @ModelAttribute("job") JobRequest request,
                             BindingResult bindingResult,
                             RedirectAttributes redirectAttributes,
                             Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("jobId", id);
            model.addAttribute("departments", jobService.getAllDepartments());
            model.addAttribute("employmentTypes", Job.EmploymentType.values());
            return "jobs/form";
        }

        Job updated = new Job();
        updated.setTitle(request.getTitle());
        updated.setLocation(request.getLocation());
        updated.setEmploymentType(request.getEmploymentType());
        updated.setSalaryMin(request.getSalaryMin());
        updated.setSalaryMax(request.getSalaryMax());
        updated.setDescription(request.getDescription());
        updated.setRequirements(request.getRequirements());
        updated.setClosingDate(request.getClosingDate());

        jobService.update(id, updated, request.getDepartmentId());
        redirectAttributes.addFlashAttribute("success", "Job updated successfully.");
        return "redirect:/recruiter/jobs";
    }

    @PostMapping("/jobs/{id}/status")
    public String changeStatus(@PathVariable Long id,
                                @RequestParam Job.Status status,
                                RedirectAttributes redirectAttributes) {
        jobService.changeStatus(id, status);
        redirectAttributes.addFlashAttribute("success",
                "Job status changed to " + status + ".");
        return "redirect:/recruiter/jobs";
    }
}
