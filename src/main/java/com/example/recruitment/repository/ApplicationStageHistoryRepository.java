package com.example.recruitment.repository;

import com.example.recruitment.entity.ApplicationStageHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ApplicationStageHistoryRepository extends JpaRepository<ApplicationStageHistory, Long> {
    List<ApplicationStageHistory> findByApplicationIdOrderByChangedAtAsc(Long applicationId);
}
