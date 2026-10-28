package com.example.recruitment.service;

import com.example.recruitment.audit.AuditService;
import com.example.recruitment.entity.Application;
import com.example.recruitment.entity.Offer;
import com.example.recruitment.entity.User;
import com.example.recruitment.notification.NotificationService;
import com.example.recruitment.repository.ApplicationRepository;
import com.example.recruitment.repository.OfferRepository;
import com.example.recruitment.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

@Service
public class OfferService {

    private final OfferRepository offerRepository;
    private final ApplicationRepository applicationRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final AuditService auditService;

    public OfferService(OfferRepository offerRepository,
                         ApplicationRepository applicationRepository,
                         UserRepository userRepository,
                         NotificationService notificationService,
                         AuditService auditService) {
        this.offerRepository = offerRepository;
        this.applicationRepository = applicationRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
        this.auditService = auditService;
    }

    @Transactional
    public Offer extendOffer(Long applicationId, BigDecimal salary, String currency,
                              LocalDate startDate, LocalDate expiryDate,
                              String notes, String creatorUsername) {
        Application app = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new IllegalArgumentException("Application not found: " + applicationId));

        offerRepository.findByApplicationId(applicationId).ifPresent(existing -> {
            throw new IllegalStateException("An offer already exists for this application.");
        });

        Offer offer = new Offer();
        offer.setApplication(app);
        offer.setSalary(salary);
        offer.setCurrency(currency != null ? currency : "INR");
        offer.setStartDate(startDate);
        offer.setExpiryDate(expiryDate);
        offer.setNotes(notes);
        offer.setStatus(Offer.Status.PENDING);
        userRepository.findByUsername(creatorUsername).ifPresent(offer::setCreatedBy);

        Offer saved = offerRepository.save(offer);

        // Move application to OFFERED stage
        app.setStage(Application.Stage.OFFERED);
        applicationRepository.save(app);

        // Notify applicant
        notificationService.send(app.getApplicant().getUser(),
                "Offer extended!",
                "Congratulations! An offer has been made for '" + app.getJob().getTitle() + "'.",
                "/applications/" + app.getId());

        auditService.log("OFFER_EXTENDED", "Offer", saved.getId(),
                "Application #" + applicationId + " by " + creatorUsername);
        return saved;
    }

    @Transactional
    public Offer respondToOffer(Long offerId, Offer.Status response, String applicantUsername) {
        Offer offer = offerRepository.findById(offerId)
                .orElseThrow(() -> new IllegalArgumentException("Offer not found: " + offerId));

        if (response != Offer.Status.ACCEPTED && response != Offer.Status.DECLINED) {
            throw new IllegalArgumentException("Only ACCEPTED or DECLINED responses are valid.");
        }

        offer.setStatus(response);
        offerRepository.save(offer);

        if (response == Offer.Status.ACCEPTED) {
            Application app = offer.getApplication();
            app.setStage(Application.Stage.HIRED);
            applicationRepository.save(app);
        }

        auditService.log("OFFER_RESPONDED", "Offer", offerId,
                "Response: " + response + " by " + applicantUsername);
        return offer;
    }

    public long countPending() {
        return offerRepository.countByStatus(Offer.Status.PENDING);
    }
}
