package com.ezielnik.api.photo;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.server.ResponseStatusException;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

class PhotoStorageServiceTest {

    @TempDir
    Path tempDir;

    private PhotoStorageService service;

    @BeforeEach
    void setUp() {
        service = new PhotoStorageService(tempDir.toString());
    }

    private MockMultipartFile validPng() throws IOException {
        BufferedImage img = new BufferedImage(10, 10, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setColor(Color.GREEN);
        g.fillRect(0, 0, 10, 10);
        g.dispose();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(img, "png", baos);
        return new MockMultipartFile("photo", "test.png", "image/png", baos.toByteArray());
    }

    // --- save() ---

    @Test
    void save_validImage_returnsUrlAndWritesFile() throws IOException {
        String url = service.save(validPng());

        assertThat(url).startsWith("/photos/").endsWith(".jpeg");
        String filename = url.substring("/photos/".length());
        assertThat(tempDir.resolve(filename)).exists();
    }

    @Test
    void save_producesReadableJpeg() throws IOException {
        String url = service.save(validPng());
        String filename = url.substring("/photos/".length());

        BufferedImage result = ImageIO.read(tempDir.resolve(filename).toFile());
        assertThat(result).isNotNull();
        assertThat(result.getWidth()).isEqualTo(10);
        assertThat(result.getHeight()).isEqualTo(10);
    }

    @Test
    void save_nullFile_throws400() {
        assertThatThrownBy(() -> service.save(null))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode())
                        .isEqualTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    void save_nonImageContentType_throws400() {
        MockMultipartFile file = new MockMultipartFile("photo", "doc.pdf", "application/pdf", new byte[]{1, 2, 3});

        assertThatThrownBy(() -> service.save(file))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode())
                        .isEqualTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    void save_corruptImageBytes_throws400() {
        MockMultipartFile file = new MockMultipartFile("photo", "bad.png", "image/png", new byte[]{0, 1, 2, 3, 4});

        assertThatThrownBy(() -> service.save(file))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode())
                        .isEqualTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    void save_imageWithAlphaChannel_flattensToWhiteBackground() throws IOException {
        // ARGB image with transparent pixels - should be flattened, not corrupt
        BufferedImage argb = new BufferedImage(8, 8, BufferedImage.TYPE_INT_ARGB);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(argb, "png", baos);
        MockMultipartFile file = new MockMultipartFile("photo", "transparent.png", "image/png", baos.toByteArray());

        String url = service.save(file);
        String filename = url.substring("/photos/".length());
        BufferedImage result = ImageIO.read(tempDir.resolve(filename).toFile());
        assertThat(result).isNotNull();
    }

    // --- delete() ---

    @Test
    void delete_removesFileFromDisk() throws IOException {
        String url = service.save(validPng());
        String filename = url.substring("/photos/".length());
        Path filePath = tempDir.resolve(filename);
        assertThat(filePath).exists();

        service.delete(url);

        assertThat(filePath).doesNotExist();
    }

    @Test
    void delete_nullUrl_doesNotThrow() {
        assertThatNoException().isThrownBy(() -> service.delete(null));
    }

    @Test
    void delete_nonexistentFile_doesNotThrow() {
        assertThatNoException().isThrownBy(() -> service.delete("/photos/no-such-file.jpeg"));
    }

    // --- savePending() ---

    @Test
    void savePending_writesFileToPendingDirWithCorrectName() throws IOException {
        String pendingId = UUID.randomUUID().toString();

        String filename = service.savePending(pendingId, validPng());

        assertThat(filename).isEqualTo(pendingId + ".jpeg");
        assertThat(tempDir.resolve("pending").resolve(filename)).exists();
    }

    @Test
    void savePending_producesReadableJpeg() throws IOException {
        String pendingId = UUID.randomUUID().toString();
        String filename = service.savePending(pendingId, validPng());

        BufferedImage result = ImageIO.read(tempDir.resolve("pending").resolve(filename).toFile());
        assertThat(result).isNotNull();
    }

    @Test
    void savePending_nullFile_throws400() {
        assertThatThrownBy(() -> service.savePending("some-id", null))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode())
                        .isEqualTo(HttpStatus.BAD_REQUEST));
    }

    // --- moveToPermanent() ---

    @Test
    void moveToPermanent_movesFileAndReturnsUrl() throws IOException {
        String pendingId = UUID.randomUUID().toString();
        String pendingFilename = service.savePending(pendingId, validPng());

        String permanentUrl = service.moveToPermanent(pendingFilename);

        assertThat(permanentUrl).startsWith("/photos/").endsWith(".jpeg");
        assertThat(tempDir.resolve("pending").resolve(pendingFilename)).doesNotExist();
        String permanentFilename = permanentUrl.substring("/photos/".length());
        assertThat(tempDir.resolve(permanentFilename)).exists();
    }

    @Test
    void moveToPermanent_pathTraversal_throws400() {
        assertThatThrownBy(() -> service.moveToPermanent("../outside.jpeg"))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode())
                        .isEqualTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    void moveToPermanent_nonexistentFile_throws500() {
        assertThatThrownBy(() -> service.moveToPermanent("nonexistent.jpeg"))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode())
                        .isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR));
    }

    // --- deletePendingFile() ---

    @Test
    void deletePendingFile_removesFileFromPendingDir() throws IOException {
        String pendingId = UUID.randomUUID().toString();
        String filename = service.savePending(pendingId, validPng());
        Path pendingPath = tempDir.resolve("pending").resolve(filename);
        assertThat(pendingPath).exists();

        service.deletePendingFile(filename);

        assertThat(pendingPath).doesNotExist();
    }

    @Test
    void deletePendingFile_nullFilename_doesNotThrow() {
        assertThatNoException().isThrownBy(() -> service.deletePendingFile(null));
    }

    @Test
    void deletePendingFile_nonexistentFile_doesNotThrow() {
        assertThatNoException().isThrownBy(() -> service.deletePendingFile("ghost.jpeg"));
    }

    // --- cleanupOrphanedPendingFiles() ---

    @Test
    void cleanupOrphanedPendingFiles_preservesRecentFiles() throws IOException {
        String filename = service.savePending(UUID.randomUUID().toString(), validPng());
        Path pendingPath = tempDir.resolve("pending").resolve(filename);

        service.cleanupOrphanedPendingFiles();

        assertThat(pendingPath).exists();
    }

    @Test
    void cleanupOrphanedPendingFiles_deletesOldFiles() throws IOException {
        String filename = service.savePending(UUID.randomUUID().toString(), validPng());
        Path pendingPath = tempDir.resolve("pending").resolve(filename);

        Files.setLastModifiedTime(pendingPath, FileTime.from(Instant.now().minus(2, ChronoUnit.HOURS)));

        service.cleanupOrphanedPendingFiles();

        assertThat(pendingPath).doesNotExist();
    }
}
