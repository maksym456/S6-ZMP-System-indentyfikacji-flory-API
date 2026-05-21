package com.ezielnik.api.photo;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class PhotoStorageService {

    private static final List<String> ALLOWED_CONTENT_TYPES = List.of(
            "image/jpeg", "image/png"
    );

    private final Path storageRoot;
    private final Path pendingRoot;

    public PhotoStorageService(@Value("${app.photo-storage-path}") String storagePath) {
        this.storageRoot = Paths.get(storagePath).toAbsolutePath().normalize();
        this.pendingRoot = storageRoot.resolve("pending");
        try {
            Files.createDirectories(storageRoot);
            Files.createDirectories(pendingRoot);
        } catch (IOException e) {
            throw new RuntimeException("Could not create photo storage directories", e);
        }
    }

    public String save(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Photo is required");
        }

        String contentType = validateContentType(file);
        String filename = UUID.randomUUID() + "." + extensionFor(contentType);

        try {
            Files.copy(file.getInputStream(), storageRoot.resolve(filename));
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to save photo");
        }

        return "/photos/" + filename;
    }

    public void delete(String photoUrl) {
        if (photoUrl == null) return;

        String filename = photoUrl.substring(photoUrl.lastIndexOf('/') + 1);
        Path filePath = storageRoot.resolve(filename).normalize();

        if (!filePath.startsWith(storageRoot)) return;

        try {
            Files.deleteIfExists(filePath);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to delete photo");
        }
    }

    public String savePending(String pendingPhotoId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Photo is required");
        }

        String contentType = validateContentType(file);
        String filename = pendingPhotoId + "." + extensionFor(contentType);

        try {
            Files.copy(file.getInputStream(), pendingRoot.resolve(filename));
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to save photo");
        }

        return filename;
    }

    public String moveToPermanent(String pendingFilename) {
        Path source = pendingRoot.resolve(pendingFilename).normalize();
        if (!source.startsWith(pendingRoot)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid pending photo reference");
        }

        String extension = pendingFilename.contains(".")
                ? pendingFilename.substring(pendingFilename.lastIndexOf('.'))
                : "";
        String permanentFilename = UUID.randomUUID() + extension;
        Path destination = storageRoot.resolve(permanentFilename).normalize();

        try {
            Files.move(source, destination);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to confirm photo");
        }

        return "/photos/" + permanentFilename;
    }

    public void deletePendingFile(String pendingFilename) {
        if (pendingFilename == null) return;
        Path filePath = pendingRoot.resolve(pendingFilename).normalize();
        if (!filePath.startsWith(pendingRoot)) return;
        try {
            Files.deleteIfExists(filePath);
        } catch (IOException ignored) {
        }
    }

    @Scheduled(fixedDelay = 300_000)
    public void cleanupOrphanedPendingFiles() {
        Instant cutoff = Instant.now().minusSeconds(3600);
        try {
            Files.list(pendingRoot).forEach(path -> {
                try {
                    BasicFileAttributes attrs = Files.readAttributes(path, BasicFileAttributes.class);
                    if (attrs.creationTime().toInstant().isBefore(cutoff)) {
                        Files.deleteIfExists(path);
                    }
                } catch (IOException ignored) {
                }
            });
        } catch (IOException ignored) {
        }
    }

    private String validateContentType(MultipartFile file) {
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only JPEG and PNG images are allowed");
        }
        return contentType;
    }

    private String extensionFor(String contentType) {
        return contentType.substring(contentType.lastIndexOf('/') + 1);
    }
}
