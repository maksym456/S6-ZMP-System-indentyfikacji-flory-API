package com.ezielnik.api.plant;

import com.ezielnik.api.photo.PhotoStorageService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class PendingPlantService {

    private final Map<String, PendingEntry> pending = new ConcurrentHashMap<>();
    private final PhotoStorageService photoStorageService;

    public PendingPlantService(PhotoStorageService photoStorageService) {
        this.photoStorageService = photoStorageService;
    }

    public String save(MultipartFile photo,
                       PlantIdentificationService.IdentificationResult identification,
                       String photoDescription) {
        String pendingPhotoId = UUID.randomUUID().toString();
        String pendingFilename = photoStorageService.savePending(pendingPhotoId, photo);
        pending.put(pendingPhotoId, new PendingEntry(pendingFilename, identification, photoDescription, Instant.now()));
        return pendingPhotoId;
    }

    public PendingEntry consume(String pendingPhotoId) {
        return pending.remove(pendingPhotoId);
    }

    @Scheduled(fixedDelay = 300_000)
    public void cleanupExpired() {
        Instant cutoff = Instant.now().minusSeconds(3600);
        pending.entrySet().removeIf(entry -> {
            if (entry.getValue().createdAt().isBefore(cutoff)) {
                photoStorageService.deletePendingFile(entry.getValue().pendingFilename());
                return true;
            }
            return false;
        });
        photoStorageService.cleanupOrphanedPendingFiles();
    }

    public record PendingEntry(
            String pendingFilename,
            PlantIdentificationService.IdentificationResult identification,
            String photoDescription,
            Instant createdAt
    ) {}
}
