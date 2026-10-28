package com.example.recruitment.repository;

import com.example.recruitment.entity.Offer;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OfferRepository extends JpaRepository<Offer, Long> {

    @Override
    @EntityGraph(attributePaths = {"application", "application.job", "application.applicant", "application.applicant.user"})
    Optional<Offer> findById(Long id);

    @EntityGraph(attributePaths = {"application", "application.job", "application.applicant", "application.applicant.user"})
    Optional<Offer> findByApplicationId(Long applicationId);

    long countByStatus(Offer.Status status);
}
