package com.ezielnik.api.plant;

import com.ezielnik.api.photo.PhotoStorageService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PendingPlantServiceTest {

    @Mock
    private PhotoStorageService photoStorageService;

    @Mock
    private MultipartFile photo;

    private void stubSavePending() {
        when(photoStorageService.savePending(anyString(), any())).thenAnswer(
                inv -> "pending-" + inv.getArgument(0) + ".jpg"
        );
    }

    @Test
    void save_returnsNonBlankPendingPhotoId() {
        stubSavePending();
        PendingPlantService service = new PendingPlantService(photoStorageService);

        String id = service.save(photo, PlantIdentificationService.IdentificationResult.empty(), "desc");

        assertThat(id).isNotBlank();
    }

    @Test
    void consume_returnsEntryWithCorrectFields() {
        stubSavePending();
        PendingPlantService service = new PendingPlantService(photoStorageService);
        PlantIdentificationService.IdentificationResult result =
                new PlantIdentificationService.IdentificationResult("Rosa canina", 0.9, "gbif-1", "Rosaceae", "Rosa", "Dog rose");

        String id = service.save(photo, result, "A red rose");

        PendingPlantService.PendingEntry entry = service.consume(id);
        assertThat(entry).isNotNull();
        assertThat(entry.pendingFilename()).isEqualTo("pending-" + id + ".jpg");
        assertThat(entry.photoDescription()).isEqualTo("A red rose");
        assertThat(entry.identification().detectedSpecies()).isEqualTo("Rosa canina");
    }

    @Test
    void consume_secondCall_returnsNull() {
        stubSavePending();
        PendingPlantService service = new PendingPlantService(photoStorageService);
        String id = service.save(photo, PlantIdentificationService.IdentificationResult.empty(), "desc");

        service.consume(id);

        assertThat(service.consume(id)).isNull();
    }

    @Test
    void consume_unknownId_returnsNull() {
        PendingPlantService service = new PendingPlantService(photoStorageService);

        assertThat(service.consume("non-existent-id")).isNull();
    }

    @Test
    void cleanupExpired_removesEntriesOlderThanOneHour() {
        stubSavePending();
        AtomicReference<Instant> clock = new AtomicReference<>(Instant.now().minusSeconds(3700));
        PendingPlantService service = new PendingPlantService(photoStorageService, clock::get);

        String id = service.save(photo, PlantIdentificationService.IdentificationResult.empty(), "old");
        String expectedFilename = "pending-" + id + ".jpg";

        clock.set(Instant.now());
        service.cleanupExpired();

        assertThat(service.consume(id)).isNull();
        verify(photoStorageService).deletePendingFile(expectedFilename);
    }

    @Test
    void cleanupExpired_keepsEntriesYoungerThanOneHour() {
        stubSavePending();
        AtomicReference<Instant> clock = new AtomicReference<>(Instant.now());
        PendingPlantService service = new PendingPlantService(photoStorageService, clock::get);

        String id = service.save(photo, PlantIdentificationService.IdentificationResult.empty(), "fresh");

        clock.set(Instant.now().plusSeconds(300));
        service.cleanupExpired();

        assertThat(service.consume(id)).isNotNull();
        verify(photoStorageService, never()).deletePendingFile(anyString());
    }

    @Test
    void cleanupExpired_callsOrphanCleanup() {
        PendingPlantService service = new PendingPlantService(photoStorageService);

        service.cleanupExpired();

        verify(photoStorageService).cleanupOrphanedPendingFiles();
    }
}
