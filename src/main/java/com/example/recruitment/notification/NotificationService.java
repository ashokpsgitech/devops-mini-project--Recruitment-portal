package com.example.recruitment.notification;

import com.example.recruitment.entity.Notification;
import com.example.recruitment.entity.User;
import com.example.recruitment.repository.NotificationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final NotificationRepository notificationRepository;
    private final JavaMailSender mailSender;

    public NotificationService(NotificationRepository notificationRepository,
                               org.springframework.beans.factory.ObjectProvider<JavaMailSender> mailSenderProvider) {
        this.notificationRepository = notificationRepository;
        this.mailSender = mailSenderProvider.getIfAvailable();
    }

    @Transactional
    public Notification send(User recipient, String title, String message, String link) {
        Notification n = new Notification();
        n.setRecipient(recipient);
        n.setTitle(title);
        n.setMessage(message);
        n.setLink(link);
        return notificationRepository.save(n);
    }

    @Transactional
    public void sendEmail(String to, String subject, String body) {
        if (mailSender == null) {
            log.info("Mail sender not available; skipping email delivery to {}", to);
            return;
        }
        try {
            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setTo(to);
            msg.setSubject("[TalentBridge] " + subject);
            msg.setText(body);
            mailSender.send(msg);
        } catch (MailException e) {
            log.warn("Failed to send email to {}: {}", to, e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public Page<Notification> getForUser(Long userId, Pageable pageable) {
        return notificationRepository.findByRecipientIdOrderByCreatedAtDesc(userId, pageable);
    }

    @Transactional(readOnly = true)
    public long countUnread(Long userId) {
        return notificationRepository.countByRecipientIdAndReadFalse(userId);
    }

    @Transactional
    public void markRead(Long notificationId, Long userId) {
        notificationRepository.findById(notificationId).ifPresent(n -> {
            if (n.getRecipient().getId().equals(userId)) {
                n.setRead(true);
            }
        });
    }

    @Transactional
    public void markAllRead(Long userId) {
        notificationRepository.markAllReadForUser(userId);
    }
}
