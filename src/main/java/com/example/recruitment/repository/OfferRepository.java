package com.example.recruitment.repository;

import com.example.recruitment.entity.Offer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OfferRepository extends JpaRepository<Offer, Long> {
    Optional<Offer> findByApplicationId(Long applicationId);
    long countByStatus(Offer.Status status);
}
