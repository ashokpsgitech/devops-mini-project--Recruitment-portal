package com.example.recruitment.dto;

import com.example.recruitment.entity.Application;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApplicationDto {
    private Long id;
    private Long jobId;
    private String jobTitle;
    private Long applicantId;
    private String applicantName;
    private String applicantEmail;
    private Application.Stage stage;
    private String coverLetter;
    private Instant appliedAt;
    private Instant updatedAt;

    public static ApplicationDto fromEntity(Application app) {
        if (app == null) return null;
        return ApplicationDto.builder()
                .id(app.getId())
                .jobId(app.getJob() != null ? app.getJob().getId() : null)
                .jobTitle(app.getJob() != null ? app.getJob().getTitle() : null)
                .applicantId(app.getApplicant() != null ? app.getApplicant().getId() : null)
                .applicantName(app.getApplicant() != null
                        ? app.getApplicant().getFullName() : null)
                .applicantEmail(app.getApplicant() != null && app.getApplicant().getUser() != null
                        ? app.getApplicant().getUser().getEmail() : null)
                .stage(app.getStage())
                .coverLetter(app.getCoverLetter())
                .appliedAt(app.getAppliedAt())
                .updatedAt(app.getUpdatedAt())
                .build();
    }
}
