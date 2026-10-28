package com.example.recruitment.dto;

import com.example.recruitment.entity.Job;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter @Setter
public class JobRequest {

    @NotBlank(message = "Job title is required")
    @Size(max = 120, message = "Title must be at most 120 characters")
    private String title;

    @NotNull(message = "Department is required")
    private Long departmentId;

    @NotBlank(message = "Location is required")
    @Size(max = 100, message = "Location must be at most 100 characters")
    private String location;

    @NotNull(message = "Employment type is required")
    private Job.EmploymentType employmentType;

    @DecimalMin(value = "0.0", inclusive = false, message = "Minimum salary must be positive")
    private BigDecimal salaryMin;

    @DecimalMin(value = "0.0", inclusive = false, message = "Maximum salary must be positive")
    private BigDecimal salaryMax;

    @NotBlank(message = "Job description is required")
    @Size(max = 10000, message = "Description must be at most 10,000 characters")
    private String description;

    @Size(max = 10000, message = "Requirements must be at most 10,000 characters")
    private String requirements;

    private LocalDate closingDate;
}
