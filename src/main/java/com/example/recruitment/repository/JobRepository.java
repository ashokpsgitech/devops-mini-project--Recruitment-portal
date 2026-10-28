package com.example.recruitment.repository;

import com.example.recruitment.entity.Job;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface JobRepository extends JpaRepository<Job, Long> {

    Page<Job> findByStatus(Job.Status status, Pageable pageable);

    Page<Job> findByStatusAndTitleContainingIgnoreCase(
            Job.Status status, String title, Pageable pageable);

    @Query("""
        SELECT j FROM Job j
        WHERE j.status = :status
          AND (CAST(:title AS string) IS NULL OR LOWER(j.title) LIKE LOWER(CONCAT('%', CAST(:title AS string), '%')))
          AND (CAST(:location AS string) IS NULL OR LOWER(j.location) LIKE LOWER(CONCAT('%', CAST(:location AS string), '%')))
        """)
    Page<Job> search(
            @Param("status") Job.Status status,
            @Param("title") String title,
            @Param("location") String location,
            Pageable pageable);

    long countByStatus(Job.Status status);

    List<Job> findTop5ByStatusOrderByCreatedAtDesc(Job.Status status);
}
