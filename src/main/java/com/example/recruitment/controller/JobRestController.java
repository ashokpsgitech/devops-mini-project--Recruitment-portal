package com.example.recruitment.controller;

import com.example.recruitment.dto.JobDto;
import com.example.recruitment.dto.JobRequest;
import com.example.recruitment.entity.Job;
import com.example.recruitment.service.JobService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/jobs")
@Tag(name = "Jobs API", description = "Endpoints for managing job postings, searches, and publishing")
public class JobRestController {

    private final JobService jobService;

    public JobRestController(JobService jobService) {
        this.jobService = jobService;
    }

    @GetMapping
    @Operation(summary = "Search published jobs", description = "Retrieves a paginated list of published job vacancies")
    public ResponseEntity<Page<JobDto>> listJobs(@RequestParam(required = false) String title,
                                                  @RequestParam(required = false) String location,
                                                  Pageable pageable) {
        Page<Job> jobs = jobService.searchPublished(title, location, pageable);
        return ResponseEntity.ok(jobs.map(JobDto::fromEntity));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get job by ID", description = "Retrieves the details of a specific job")
    public ResponseEntity<JobDto> getJobById(@PathVariable Long id) {
        Job job = jobService.findByIdOrThrow(id);
        return ResponseEntity.ok(JobDto.fromEntity(job));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('RECRUITER', 'ADMIN')")
    @Operation(summary = "Create a job", description = "Recruiter creates a new job posting")
    public ResponseEntity<JobDto> createJob(@Valid @RequestBody JobRequest request, Authentication auth) {
        Job job = new Job();
        job.setTitle(request.getTitle());
        job.setLocation(request.getLocation());
        job.setEmploymentType(request.getEmploymentType());
        job.setSalaryMin(request.getSalaryMin());
        job.setSalaryMax(request.getSalaryMax());
        job.setDescription(request.getDescription());
        job.setRequirements(request.getRequirements());
        job.setClosingDate(request.getClosingDate());

        Job created = jobService.create(job, request.getDepartmentId(), auth.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(JobDto.fromEntity(created));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('RECRUITER', 'ADMIN')")
    @Operation(summary = "Update an existing job", description = "Recruiter updates job details")
    public ResponseEntity<JobDto> updateJob(@PathVariable Long id, @Valid @RequestBody JobRequest request) {
        Job job = new Job();
        job.setTitle(request.getTitle());
        job.setLocation(request.getLocation());
        job.setEmploymentType(request.getEmploymentType());
        job.setSalaryMin(request.getSalaryMin());
        job.setSalaryMax(request.getSalaryMax());
        job.setDescription(request.getDescription());
        job.setRequirements(request.getRequirements());
        job.setClosingDate(request.getClosingDate());

        Job updated = jobService.update(id, job, request.getDepartmentId());
        return ResponseEntity.ok(JobDto.fromEntity(updated));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('RECRUITER', 'ADMIN')")
    @Operation(summary = "Change job status", description = "Publish, close, or archive a job")
    public ResponseEntity<JobDto> changeStatus(@PathVariable Long id, @RequestParam Job.Status status) {
        jobService.changeStatus(id, status);
        Job job = jobService.findByIdOrThrow(id);
        return ResponseEntity.ok(JobDto.fromEntity(job));
    }
}
