package com.ezielnik.api.plant;

import com.ezielnik.api.photo.PhotoStorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

@Service
public class PendingPlantService {

    private final Map<String, PendingEntry> pending = new ConcurrentHashMap<>();
    private final PhotoStorageService photoStorageService;
    private final Supplier<Instant> clock;

    @Autowired
    public PendingPlantService(PhotoStorageService photoStorageService) {
        this(photoStorageService, Instant::now);
    }

    PendingPlantService(PhotoStorageService photoStorageService, Supplier<Instant> clock) {
        this.photoStorageService = photoStorageService;
        this.clock = clock;
    }

    public String save(MultipartFile photo,
                       PlantIdentificationService.IdentificationResult identification,
                       String photoDescription) {
        String pendingPhotoId = UUID.randomUUID().toString();
        String pendingFilename = photoStorageService.savePending(pendingPhotoId, photo);
        pending.put(pendingPhotoId, new PendingEntry(pendingFilename, identification, photoDescription, clock.get()));
        return pendingPhotoId;
    }

    public PendingEntry consume(String pendingPhotoId) {
        return pending.remove(pendingPhotoId);
    }

    @Scheduled(fixedDelay = 300_000)
    public void cleanupExpired() {
        Instant cutoff = clock.get().minusSeconds(3600);
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
