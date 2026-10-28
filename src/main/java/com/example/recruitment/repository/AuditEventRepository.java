package com.example.recruitment.repository;

import com.example.recruitment.entity.AuditEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditEventRepository extends JpaRepository<AuditEvent, Long> {
    Page<AuditEvent> findAllByOrderByCreatedAtDesc(Pageable pageable);
    Page<AuditEvent> findByActorUsernameContainingIgnoreCaseOrderByCreatedAtDesc(
            String username, Pageable pageable);
}
