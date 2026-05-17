package com.ezielnik.api.photo;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

@Service
public class PhotoStorageService {

    private static final List<String> ALLOWED_CONTENT_TYPES = List.of(
            "image/jpeg", "image/png"
    );

    private final Path storageRoot;

    public PhotoStorageService(@Value("${app.photo-storage-path}") String storagePath) {
        this.storageRoot = Paths.get(storagePath).toAbsolutePath().normalize();
        try {
            Files.createDirectories(storageRoot);
        } catch (IOException e) {
            throw new RuntimeException("Could not create photo storage directory", e);
        }
    }

    public String save(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Photo is required");
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only JPEG and PNG images are allowed");
        }

        String extension = contentType.substring(contentType.lastIndexOf('/') + 1);
        String filename = UUID.randomUUID() + "." + extension;

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
}
