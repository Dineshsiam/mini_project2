package com.farmers.recommendation.service;

import com.farmers.recommendation.dto.SchemeSyncRequest;
import com.farmers.recommendation.entity.Scheme;
import com.farmers.recommendation.repository.SchemeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class SchemeSyncService {

    private final SchemeRepository schemeRepository;
    private final SchemeService schemeService; // To reuse the generic save/embedding logic

    /**
     * Receives a scheme payload, checks if it has changed (via content hash),
     * and updates + re-embeds it if necessary.
     */
    @Transactional
    public String syncScheme(SchemeSyncRequest request) {
        // Calculate hash if not provided
        String currentHash = request.getContentHash();
        if (currentHash == null || currentHash.isBlank()) {
            currentHash = calculateHash(request);
        }

        // Check if scheme exists by slug (which acts as the external ID here)
        // Since we don't have a findBySlug, let's search via Java stream for simplicity 
        // given this is a mini project (production would use a @Query).
        Optional<Scheme> existingOpt = schemeRepository.findAll().stream()
                .filter(s -> s.getSlug().equals(request.getSchemeId()))
                .findFirst();

        if (existingOpt.isPresent()) {
            Scheme existing = existingOpt.get();
            if (currentHash.equals(existing.getContentHash())) {
                log.info("Scheme {} (slug={}) has not changed. Skipping sync.", existing.getName(), existing.getSlug());
                return "SKIPPED - Hash matched";
            }

            // Update existing
            updateFields(existing, request, currentHash);
            schemeService.saveScheme(existing); // This will re-compute and upsert the embedding
            log.info("Scheme {} updated successfully.", existing.getSlug());
            return "UPDATED - Hash mismatched";
        } else {
            // Create new
            Scheme newScheme = new Scheme();
            newScheme.setId(UUID.randomUUID());
            newScheme.setSlug(request.getSchemeId());
            updateFields(newScheme, request, currentHash);
            schemeService.saveScheme(newScheme);
            log.info("Scheme {} created successfully.", newScheme.getSlug());
            return "CREATED - New scheme";
        }
    }

    private void updateFields(Scheme scheme, SchemeSyncRequest request, String hash) {
        scheme.setName(request.getTitle() != null ? request.getTitle() : "Unknown");
        scheme.setDescription(request.getDescription());
        scheme.setEligibility(request.getEligibility());
        scheme.setBenefits(request.getBenefits());
        scheme.setSourceUrl(request.getSourceUrl());
        scheme.setState(request.getState() != null ? request.getState() : "Central");
        scheme.setLevel(request.getLevel() != null ? request.getLevel() : "State");
        scheme.setDepartment(request.getDepartment());
        
        scheme.setContentHash(hash);
        scheme.setLastScrapedAt(OffsetDateTime.now());
        scheme.setStatus("ACTIVE");
    }

    private String calculateHash(SchemeSyncRequest req) {
        String data = String.join("|",
                req.getTitle() != null ? req.getTitle() : "",
                req.getDescription() != null ? req.getDescription() : "",
                req.getEligibility() != null ? req.getEligibility() : "",
                req.getBenefits() != null ? req.getBenefits() : "",
                req.getSourceUrl() != null ? req.getSourceUrl() : ""
        );
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder(2 * hashBytes.length);
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }
}
