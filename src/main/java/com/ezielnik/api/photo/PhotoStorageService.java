package com.ezielnik.api.photo;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Instant;
import java.util.Iterator;
import java.util.UUID;

@Service
public class PhotoStorageService {

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

        BufferedImage image = readImage(file);
        String filename = UUID.randomUUID() + ".jpeg";
        writeJpeg(image, storageRoot.resolve(filename));

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

        BufferedImage image = readImage(file);
        String filename = pendingPhotoId + ".jpeg";
        writeJpeg(image, pendingRoot.resolve(filename));

        return filename;
    }

    public String moveToPermanent(String pendingFilename) {
        Path source = pendingRoot.resolve(pendingFilename).normalize();
        if (!source.startsWith(pendingRoot)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid pending photo reference");
        }

        String permanentFilename = UUID.randomUUID() + ".jpeg";
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
        try (var files = Files.list(pendingRoot)) {
            files.forEach(path -> {
                try {
                    BasicFileAttributes attrs = Files.readAttributes(path, BasicFileAttributes.class);
                    if (attrs.lastModifiedTime().toInstant().isBefore(cutoff)) {
                        Files.deleteIfExists(path);
                    }
                } catch (IOException ignored) {
                }
            });
        } catch (IOException ignored) {
        }
    }

    private BufferedImage readImage(MultipartFile file) {
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only image files are allowed");
        }

        try {
            BufferedImage image = ImageIO.read(file.getInputStream());
            if (image == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported or corrupt image format");
            }
            return image;
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to read image");
        }
    }

    private void writeJpeg(BufferedImage image, Path target) {
        // JPEG has no alpha channel — flatten transparency onto white background
        BufferedImage rgb = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g = rgb.createGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, rgb.getWidth(), rgb.getHeight());
        g.drawImage(image, 0, 0, null);
        g.dispose();

        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpeg");
        if (!writers.hasNext()) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "JPEG writer not available");
        }
        ImageWriter writer = writers.next();
        ImageWriteParam param = writer.getDefaultWriteParam();
        param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
        param.setCompressionQuality(0.85f);

        try (ImageOutputStream out = ImageIO.createImageOutputStream(target.toFile())) {
            writer.setOutput(out);
            writer.write(null, new IIOImage(rgb, null, null), param);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to save photo");
        } finally {
            writer.dispose();
        }
    }
}
