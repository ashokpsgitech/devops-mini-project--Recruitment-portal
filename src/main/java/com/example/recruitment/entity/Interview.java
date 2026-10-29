package com.example.recruitment.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "interviews")
@Getter @Setter @NoArgsConstructor
public class Interview {

    public enum Status { SCHEDULED, COMPLETED, CANCELLED, RESCHEDULED }
    public enum InterviewType { VIDEO, IN_PERSON, PHONE, TECHNICAL }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "application_id", nullable = false)
    private Application application;

    @Column(name = "scheduled_at", nullable = false)
    private Instant scheduledAt;

    @Column(name = "duration_mins", nullable = false)
    private Integer durationMins = 60;

    @Column(length = 500)
    private String location;

    @Enumerated(EnumType.STRING)
    @Column(name = "interview_type", nullable = false, length = 30)
    private InterviewType interviewType = InterviewType.VIDEO;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "interviewer_id")
    private User interviewer;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status = Status.SCHEDULED;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    @PreUpdate
    void onUpdate() { this.updatedAt = Instant.now(); }

    // Template compatibility accessors
    public InterviewType getType() {
        return this.interviewType;
    }

    public void setType(InterviewType type) {
        this.interviewType = type;
    }

    public Integer getDurationMinutes() {
        return this.durationMins;
    }

    public void setDurationMinutes(Integer durationMinutes) {
        this.durationMins = durationMinutes;
    }

    public String getLocationOrUrl() {
        return this.location;
    }

    public void setLocationOrUrl(String locationOrUrl) {
        this.location = locationOrUrl;
    }
}
