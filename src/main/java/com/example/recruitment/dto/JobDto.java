package com.example.recruitment.dto;

import com.example.recruitment.entity.Job;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobDto {
    private Long id;
    private String title;
    private String departmentName;
    private String location;
    private Job.EmploymentType employmentType;
    private BigDecimal salaryMin;
    private BigDecimal salaryMax;
    private String description;
    private String requirements;
    private Job.Status status;
    private LocalDate closingDate;
    private Instant createdAt;
    private Instant publishedAt;

    public static JobDto fromEntity(Job job) {
        if (job == null) return null;
        return JobDto.builder()
                .id(job.getId())
                .title(job.getTitle())
                .departmentName(job.getDepartment() != null ? job.getDepartment().getName() : null)
                .location(job.getLocation())
                .employmentType(job.getEmploymentType())
                .salaryMin(job.getSalaryMin())
                .salaryMax(job.getSalaryMax())
                .description(job.getDescription())
                .requirements(job.getRequirements())
                .status(job.getStatus())
                .closingDate(job.getClosingDate())
                .createdAt(job.getCreatedAt())
                .publishedAt(job.getPublishedAt())
                .build();
    }
}
