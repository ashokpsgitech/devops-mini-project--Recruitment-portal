package com.example.recruitment.service;

import com.example.recruitment.audit.AuditService;
import com.example.recruitment.entity.*;
import com.example.recruitment.notification.NotificationService;
import com.example.recruitment.repository.ApplicationRepository;
import com.example.recruitment.repository.OfferRepository;
import com.example.recruitment.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OfferServiceTest {

    @Mock private OfferRepository offerRepository;
    @Mock private ApplicationRepository applicationRepository;
    @Mock private UserRepository userRepository;
    @Mock private NotificationService notificationService;
    @Mock private AuditService auditService;

    private OfferService offerService;

    @BeforeEach
    void setUp() {
        offerService = new OfferService(
                offerRepository, applicationRepository, userRepository,
                notificationService, auditService);
    }

    @Test
    @DisplayName("Should successfully extend offer and move application stage to OFFERED")
    void testExtendOfferSuccess() {
        Long appId = 1L;
        Application app = new Application();
        app.setId(appId);
        app.setStage(Application.Stage.INTERVIEW);

        Job job = new Job();
        job.setTitle("DevOps Lead");
        app.setJob(job);

        User applicantUser = new User();
        applicantUser.setUsername("candidate1");
        ApplicantProfile profile = new ApplicantProfile();
        profile.setUser(applicantUser);
        app.setApplicant(profile);

        when(applicationRepository.findById(appId)).thenReturn(Optional.of(app));
        when(offerRepository.findByApplicationId(appId)).thenReturn(Optional.empty());
        when(offerRepository.save(any(Offer.class))).thenAnswer(inv -> {
            Offer o = inv.getArgument(0);
            o.setId(10L);
            return o;
        });

        Offer offer = offerService.extendOffer(
                appId, new BigDecimal("2500000"), "INR",
                LocalDate.now().plusWeeks(2), LocalDate.now().plusWeeks(1),
                "Includes bonus", "recruiter1");

        assertNotNull(offer);
        assertEquals(Offer.Status.PENDING, offer.getStatus());
        assertEquals(Application.Stage.OFFERED, app.getStage());
        verify(notificationService, times(1)).send(eq(applicantUser), any(), any(), any());
        verify(auditService, times(1)).log(eq("OFFER_EXTENDED"), eq("Offer"), eq(10L), any());
    }

    @Test
    @DisplayName("Accepting offer should move application to HIRED")
    void testRespondToOfferAccepted() {
        Long offerId = 10L;
        Application app = new Application();
        app.setId(1L);
        app.setStage(Application.Stage.OFFERED);

        Offer offer = new Offer();
        offer.setId(offerId);
        offer.setApplication(app);
        offer.setStatus(Offer.Status.PENDING);

        when(offerRepository.findById(offerId)).thenReturn(Optional.of(offer));

        Offer result = offerService.respondToOffer(offerId, Offer.Status.ACCEPTED, "candidate1");

        assertEquals(Offer.Status.ACCEPTED, result.getStatus());
        assertEquals(Application.Stage.HIRED, app.getStage());
        verify(applicationRepository, times(1)).save(app);
        verify(auditService, times(1)).log(eq("OFFER_RESPONDED"), eq("Offer"), eq(offerId), any());
    }
}
