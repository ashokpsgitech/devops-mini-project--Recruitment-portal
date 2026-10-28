package com.example.recruitment.service;

import com.example.recruitment.audit.AuditService;
import com.example.recruitment.entity.Department;
import com.example.recruitment.entity.Job;
import com.example.recruitment.entity.User;
import com.example.recruitment.repository.DepartmentRepository;
import com.example.recruitment.repository.JobRepository;
import com.example.recruitment.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
public class JobService {

    private final JobRepository jobRepository;
    private final DepartmentRepository departmentRepository;
    private final UserRepository userRepository;
    private final AuditService auditService;

    public JobService(JobRepository jobRepository,
                      DepartmentRepository departmentRepository,
                      UserRepository userRepository,
                      AuditService auditService) {
        this.jobRepository = jobRepository;
        this.departmentRepository = departmentRepository;
        this.userRepository = userRepository;
        this.auditService = auditService;
    }

    // -----------------------------------------------------------------------
    // PUBLIC / APPLICANT READ
    // -----------------------------------------------------------------------

    @Transactional(readOnly = true)
    public Page<Job> searchPublished(String title, String location, Pageable pageable) {
        String cleanTitle = (title != null && !title.isBlank()) ? title.trim() : null;
        String cleanLocation = (location != null && !location.isBlank()) ? location.trim() : null;
        if (cleanTitle == null && cleanLocation == null) {
            return jobRepository.findByStatus(Job.Status.PUBLISHED, pageable);
        }
        if (cleanTitle != null && cleanLocation == null) {
            return jobRepository.findByStatusAndTitleContainingIgnoreCase(Job.Status.PUBLISHED, cleanTitle, pageable);
        }
        return jobRepository.search(Job.Status.PUBLISHED, cleanTitle, cleanLocation, pageable);
    }

    @Transactional(readOnly = true)
    public Job getPublishedJobOrThrow(Long id) {
        Job job = jobRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Job not found: " + id));
        if (job.getStatus() != Job.Status.PUBLISHED) {
            throw new IllegalArgumentException("Job is not publicly available.");
        }
        return job;
    }

    // -----------------------------------------------------------------------
    // RECRUITER CRUD
    // -----------------------------------------------------------------------

    @Transactional(readOnly = true)
    public Page<Job> findAll(Pageable pageable) {
        return jobRepository.findAll(pageable);
    }

    @Transactional(readOnly = true)
    public Job findByIdOrThrow(Long id) {
        return jobRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Job not found: " + id));
    }

    @Transactional
    @PreAuthorize("hasRole('RECRUITER')")
    public Job create(Job job, Long departmentId, String creatorUsername) {
        departmentRepository.findById(departmentId)
                .ifPresent(job::setDepartment);
        userRepository.findByUsername(creatorUsername)
                .ifPresent(job::setCreatedBy);
        job.setStatus(Job.Status.DRAFT);
        Job saved = jobRepository.save(job);
        auditService.log("JOB_CREATED", "Job", saved.getId(), "Created: " + saved.getTitle());
        return saved;
    }

    @Transactional
    @PreAuthorize("hasRole('RECRUITER')")
    public Job update(Long id, Job updated, Long departmentId) {
        Job existing = findByIdOrThrow(id);
        existing.setTitle(updated.getTitle());
        existing.setLocation(updated.getLocation());
        existing.setEmploymentType(updated.getEmploymentType());
        existing.setSalaryMin(updated.getSalaryMin());
        existing.setSalaryMax(updated.getSalaryMax());
        existing.setDescription(updated.getDescription());
        existing.setRequirements(updated.getRequirements());
        existing.setClosingDate(updated.getClosingDate());
        departmentRepository.findById(departmentId)
                .ifPresent(existing::setDepartment);
        auditService.log("JOB_UPDATED", "Job", id, "Updated: " + existing.getTitle());
        return jobRepository.save(existing);
    }

    @Transactional
    @PreAuthorize("hasRole('RECRUITER')")
    public void changeStatus(Long id, Job.Status newStatus) {
        Job job = findByIdOrThrow(id);
        Job.Status old = job.getStatus();
        job.setStatus(newStatus);
        if (newStatus == Job.Status.PUBLISHED && job.getPublishedAt() == null) {
            job.setPublishedAt(Instant.now());
        }
        jobRepository.save(job);
        auditService.log("JOB_STATUS_CHANGED", "Job", id,
                old + " -> " + newStatus + " for: " + job.getTitle());
    }

    // -----------------------------------------------------------------------
    // DASHBOARD STATS
    // -----------------------------------------------------------------------

    public long countByStatus(Job.Status status) {
        return jobRepository.countByStatus(status);
    }

    public List<Job> getRecentPublished() {
        return jobRepository.findTop5ByStatusOrderByCreatedAtDesc(Job.Status.PUBLISHED);
    }

    public List<Department> getAllDepartments() {
        return departmentRepository.findAll();
    }
}
