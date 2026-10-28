package com.example.recruitment.storage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.util.UUID;

/**
 * Stores resumes on the local filesystem under a configurable private directory.
 * This directory MUST be outside the application's static resource paths.
 * Uses random UUIDs as filenames to prevent path traversal and filename collisions.
 */
@Service
public class LocalStorageService implements StorageService {

    private final Path storageRoot;

    public LocalStorageService(
            @Value("${app.storage.local.path:${user.home}/.talentbridge/resumes}") String path) {
        this.storageRoot = Paths.get(path).toAbsolutePath().normalize();
        try {
            Files.createDirectories(storageRoot);
        } catch (IOException e) {
            throw new IllegalStateException("Cannot create resume storage directory: " + storageRoot, e);
        }
    }

    @Override
    public String store(MultipartFile file, String applicantId) throws IOException {
        String sanitizedApplicantId = applicantId.replaceAll("[^a-zA-Z0-9_-]", "_");
        Path applicantDir = storageRoot.resolve(sanitizedApplicantId);
        Files.createDirectories(applicantDir);

        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf('.'));
        }

        String storedFilename = UUID.randomUUID() + extension;
        Path target = applicantDir.resolve(storedFilename);

        // Security: ensure resolved path is still under storageRoot
        if (!target.normalize().startsWith(storageRoot)) {
            throw new SecurityException("Path traversal attempt detected");
        }

        Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
        return sanitizedApplicantId + "/" + storedFilename;
    }

    @Override
    public InputStream load(String storedPath) throws IOException {
        Path target = storageRoot.resolve(storedPath).normalize();
        if (!target.startsWith(storageRoot)) {
            throw new SecurityException("Path traversal attempt detected");
        }
        return Files.newInputStream(target);
    }

    @Override
    public void delete(String storedPath) {
        try {
            Path target = storageRoot.resolve(storedPath).normalize();
            if (target.startsWith(storageRoot)) {
                Files.deleteIfExists(target);
            }
        } catch (IOException e) {
            // Log and continue — deletion failure should not break business logic
            org.slf4j.LoggerFactory.getLogger(getClass())
                .warn("Failed to delete stored file: {}", storedPath, e);
        }
    }
}
