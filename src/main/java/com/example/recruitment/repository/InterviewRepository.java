package com.example.recruitment.repository;

import com.example.recruitment.entity.Interview;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface InterviewRepository extends JpaRepository<Interview, Long> {

    @Override
    @EntityGraph(attributePaths = {"application", "application.job", "application.applicant", "application.applicant.user", "interviewer"})
    Optional<Interview> findById(Long id);

    @EntityGraph(attributePaths = {"application", "application.job", "application.applicant", "application.applicant.user", "interviewer"})
    List<Interview> findByApplicationId(Long applicationId);

    @EntityGraph(attributePaths = {"application", "application.job", "application.applicant", "application.applicant.user", "interviewer"})
    @Query("""
        SELECT i FROM Interview i
        WHERE i.interviewer.id = :userId
          AND i.scheduledAt >= :from
          AND i.status = 'SCHEDULED'
        ORDER BY i.scheduledAt ASC
        """)
    List<Interview> findUpcomingForInterviewer(
            @Param("userId") Long userId,
            @Param("from") Instant from);

    @EntityGraph(attributePaths = {"application", "application.job", "application.applicant", "application.applicant.user", "interviewer"})
    @Query("""
        SELECT i FROM Interview i
        WHERE i.application.applicant.user.id = :userId
          AND i.scheduledAt >= :from
          AND i.status = 'SCHEDULED'
        ORDER BY i.scheduledAt ASC
        """)
    List<Interview> findUpcomingForApplicant(
            @Param("userId") Long userId,
            @Param("from") Instant from);
}
