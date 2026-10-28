package com.example.recruitment.controller;

import com.example.recruitment.dto.ApplicationDto;
import com.example.recruitment.entity.Application;
import com.example.recruitment.service.ApplicationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/applications")
@Tag(name = "Applications API", description = "Endpoints for applicant submissions and pipeline progression")
public class ApplicationRestController {

    private final ApplicationService applicationService;

    public ApplicationRestController(ApplicationService applicationService) {
        this.applicationService = applicationService;
    }

    @GetMapping("/job/{jobId}")
    @PreAuthorize("hasAnyRole('RECRUITER', 'ADMIN', 'HIRING_MANAGER')")
    @Operation(summary = "Get applications for a job", description = "Retrieves all candidate applications for a specific job")
    public ResponseEntity<Page<ApplicationDto>> getApplicationsByJob(@PathVariable Long jobId, Pageable pageable) {
        Page<Application> apps = applicationService.findByJob(jobId, pageable);
        return ResponseEntity.ok(apps.map(ApplicationDto::fromEntity));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get application details", description = "Retrieves details of an individual application")
    public ResponseEntity<ApplicationDto> getApplicationById(@PathVariable Long id) {
        Application app = applicationService.findByIdOrThrow(id);
        return ResponseEntity.ok(ApplicationDto.fromEntity(app));
    }

    @PostMapping("/{id}/stage")
    @PreAuthorize("hasAnyRole('RECRUITER', 'ADMIN')")
    @Operation(summary = "Update application stage", description = "Transition candidate across recruitment workflow stages")
    public ResponseEntity<ApplicationDto> updateStage(@PathVariable Long id,
                                                      @RequestParam Application.Stage stage,
                                                      @RequestParam(required = false) String notes,
                                                      Authentication auth) {
        applicationService.changeStage(id, stage, auth.getName(), notes);
        Application app = applicationService.findByIdOrThrow(id);
        return ResponseEntity.ok(ApplicationDto.fromEntity(app));
    }
}
