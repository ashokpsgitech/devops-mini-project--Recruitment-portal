package com.example.recruitment.repository;

import com.example.recruitment.entity.Application;
import com.example.recruitment.entity.Application.Stage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ApplicationRepository extends JpaRepository<Application, Long> {

    Optional<Application> findByJobIdAndApplicantId(Long jobId, Long applicantId);
    boolean existsByJobIdAndApplicantId(Long jobId, Long applicantId);

    Page<Application> findByApplicantId(Long applicantId, Pageable pageable);
    Page<Application> findByJobId(Long jobId, Pageable pageable);
    Page<Application> findByJobIdAndStage(Long jobId, Stage stage, Pageable pageable);

    List<Application> findByApplicantId(Long applicantId);
    List<Application> findByJobIdOrderByAppliedAtDesc(Long jobId);

    long countByStage(Stage stage);
    long countByJobId(Long jobId);

    @Query("SELECT COUNT(a) FROM Application a WHERE a.applicant.id = :applicantId")
    long countByApplicantId(@Param("applicantId") Long applicantId);

    @Query("SELECT a.stage, COUNT(a) FROM Application a GROUP BY a.stage")
    List<Object[]> countByStageGrouped();

    @Query("""
        SELECT a FROM Application a
        WHERE a.job.id IN (SELECT j.id FROM Job j WHERE j.createdBy.id = :recruiterId)
        ORDER BY a.appliedAt DESC
        """)
    List<Application> findRecentByRecruiter(@Param("recruiterId") Long recruiterId, Pageable pageable);
}
