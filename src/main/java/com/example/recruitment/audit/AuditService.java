package com.example.recruitment.audit;

import com.example.recruitment.entity.AuditEvent;
import com.example.recruitment.entity.User;
import com.example.recruitment.repository.AuditEventRepository;
import com.example.recruitment.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditService {

    private final AuditEventRepository auditEventRepository;
    private final UserRepository userRepository;

    public AuditService(AuditEventRepository auditEventRepository,
                        UserRepository userRepository) {
        this.auditEventRepository = auditEventRepository;
        this.userRepository = userRepository;
    }

    /**
     * Records an audit event. Uses REQUIRES_NEW so audit is never rolled back
     * even if the calling transaction fails.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void log(String eventType, String entityType, Long entityId, String detail) {
        AuditEvent event = new AuditEvent();
        event.setEventType(eventType);
        event.setEntityType(entityType);
        event.setEntityId(entityId);
        event.setDetail(detail);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
            String username = auth.getName();
            event.setActorUsername(username);
            userRepository.findByUsername(username).ifPresent(event::setActor);
        }

        auditEventRepository.save(event);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void log(String eventType, String detail) {
        log(eventType, null, null, detail);
    }
}
