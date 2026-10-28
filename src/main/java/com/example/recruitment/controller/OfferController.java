package com.example.recruitment.controller;

import com.example.recruitment.entity.Application;
import com.example.recruitment.entity.Offer;
import com.example.recruitment.entity.User;
import com.example.recruitment.repository.OfferRepository;
import com.example.recruitment.repository.UserRepository;
import com.example.recruitment.service.ApplicationService;
import com.example.recruitment.service.OfferService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDate;

@Controller
@RequestMapping("/offers")
public class OfferController {

    private final OfferService offerService;
    private final OfferRepository offerRepository;
    private final ApplicationService applicationService;
    private final UserRepository userRepository;

    public OfferController(OfferService offerService,
                           OfferRepository offerRepository,
                           ApplicationService applicationService,
                           UserRepository userRepository) {
        this.offerService = offerService;
        this.offerRepository = offerRepository;
        this.applicationService = applicationService;
        this.userRepository = userRepository;
    }

    @GetMapping
    public String listOffers(Authentication auth, Model model) {
        boolean isRecruiterOrAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_RECRUITER") || a.getAuthority().equals("ROLE_ADMIN"));

        if (isRecruiterOrAdmin) {
            model.addAttribute("offers", offerRepository.findAll());
        } else {
            User user = userRepository.findByUsername(auth.getName()).orElseThrow();
            model.addAttribute("offers", offerRepository.findAll().stream()
                    .filter(o -> o.getApplication().getApplicant().getUser().getId().equals(user.getId()))
                    .toList());
        }
        return "offers/list";
    }

    @GetMapping("/create/{applicationId}")
    public String createOfferForm(@PathVariable Long applicationId, Model model) {
        Application application = applicationService.findByIdOrThrow(applicationId);
        model.addAttribute("application", application);
        model.addAttribute("app", application);
        return "offers/create";
    }

    @PostMapping("/create/{applicationId}")
    public String createOffer(@PathVariable Long applicationId,
                              @RequestParam BigDecimal salary,
                              @RequestParam(defaultValue = "INR") String currency,
                              @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                              @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate expiryDate,
                              @RequestParam(required = false) String notes,
                              Authentication auth,
                              RedirectAttributes redirectAttributes) {
        Offer offer = offerService.extendOffer(
                applicationId, salary, currency, startDate, expiryDate, notes, auth.getName());
        redirectAttributes.addFlashAttribute("success",
                "Job offer extended to candidate: " + offer.getApplication().getApplicant().getFullName());
        return "redirect:/applications/" + applicationId;
    }

    @GetMapping("/{id}")
    public String offerDetail(@PathVariable Long id, Authentication auth, Model model) {
        Offer offer = offerRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Offer not found: " + id));

        User current = userRepository.findByUsername(auth.getName()).orElseThrow();
        boolean isOwner = offer.getApplication().getApplicant().getUser().getId().equals(current.getId());
        boolean isPrivileged = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_RECRUITER") || a.getAuthority().equals("ROLE_ADMIN"));

        if (!isOwner && !isPrivileged) {
            throw new AccessDeniedException("Access denied.");
        }

        model.addAttribute("offer", offer);
        model.addAttribute("isApplicant", isOwner);
        return "offers/detail";
    }

    @PostMapping("/{id}/respond")
    public String respondToOffer(@PathVariable Long id,
                                 @RequestParam Offer.Status response,
                                 Authentication auth,
                                 RedirectAttributes redirectAttributes) {
        Offer offer = offerRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Offer not found: " + id));

        User current = userRepository.findByUsername(auth.getName()).orElseThrow();
        if (!offer.getApplication().getApplicant().getUser().getId().equals(current.getId())) {
            throw new AccessDeniedException("Only the applicant can accept or decline this offer.");
        }

        offerService.respondToOffer(id, response, auth.getName());
        redirectAttributes.addFlashAttribute("success", "You have successfully " + response + " the offer!");
        return "redirect:/offers/" + id;
    }
}
