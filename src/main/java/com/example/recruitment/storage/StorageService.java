package com.example.recruitment.storage;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;

/**
 * Abstraction for resume file storage.
 * Local filesystem implementation is used in development;
 * Azure Blob Storage implementation can be swapped in for cloud deployments.
 */
public interface StorageService {

    /**
     * Store a resume file and return the opaque path/key used to retrieve it.
     * The returned path MUST NOT be publicly accessible.
     */
    String store(MultipartFile file, String applicantId) throws IOException;

    /**
     * Open an InputStream for reading the stored file.
     * Caller is responsible for closing the stream.
     */
    InputStream load(String storedPath) throws IOException;

    /**
     * Delete a stored file. Silently ignores missing files.
     */
    void delete(String storedPath);
}
