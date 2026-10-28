package com.example.recruitment.repository;

import com.example.recruitment.entity.Resume;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ResumeRepository extends JpaRepository<Resume, Long> {
    List<Resume> findByApplicantIdOrderByUploadedAtDesc(Long applicantId);
    Optional<Resume> findByIdAndApplicantId(Long id, Long applicantId);
    Optional<Resume> findByApplicantIdAndActiveTrue(Long applicantId);
}
