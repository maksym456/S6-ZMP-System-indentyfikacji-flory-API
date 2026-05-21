package com.ezielnik.api.photo;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import com.ezielnik.api.friend.FriendshipRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.net.MalformedURLException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/photos")
public class PhotoController {

    private static final Map<String, MediaType> MEDIA_TYPES = Map.of(
            "jpg", MediaType.IMAGE_JPEG,
            "jpeg", MediaType.IMAGE_JPEG,
            "png", MediaType.IMAGE_PNG
    );

    private final PlantPhotoRepository plantPhotoRepository;
    private final FriendshipRepository friendshipRepository;
    private final Path storageRoot;

    public PhotoController(PlantPhotoRepository plantPhotoRepository,
                           FriendshipRepository friendshipRepository,
                           @Value("${app.photo-storage-path}") String storagePath) {
        this.plantPhotoRepository = plantPhotoRepository;
        this.friendshipRepository = friendshipRepository;
        this.storageRoot = Paths.get(storagePath).toAbsolutePath().normalize();
    }

    @Operation(summary = "Serve a plant photo file")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Photo returned"),
            @ApiResponse(responseCode = "403", description = "Photo belongs to a private herbarium"),
            @ApiResponse(responseCode = "404", description = "Photo not found")
    })
    @GetMapping("/{filename:.+}")
    public ResponseEntity<Resource> getPhoto(@PathVariable String filename) {
        String photoUrl = "/photos/" + filename;

        PlantPhoto plantPhoto = plantPhotoRepository.findByUrl(photoUrl)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Photo not found"));

        if (!plantPhoto.getPlant().getHerbarium().isPublic()) {
            UUID authenticatedUserId = getAuthenticatedUserId();
            UUID ownerId = plantPhoto.getPlant().getHerbarium().getUserId();
            if (authenticatedUserId == null
                    || (!ownerId.equals(authenticatedUserId)
                        && !friendshipRepository.areFriends(authenticatedUserId, ownerId))) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You cannot access this photo");
            }
        }

        Path filePath = storageRoot.resolve(filename).normalize();
        if (!filePath.startsWith(storageRoot)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Photo not found");
        }

        try {
            Resource resource = new UrlResource(filePath.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Photo not found");
            }

            String extension = filename.contains(".")
                    ? filename.substring(filename.lastIndexOf('.') + 1).toLowerCase()
                    : "";
            MediaType mediaType = MEDIA_TYPES.getOrDefault(extension, MediaType.APPLICATION_OCTET_STREAM);

            return ResponseEntity.ok().contentType(mediaType).body(resource);

        } catch (MalformedURLException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Photo not found");
        }
    }

    private UUID getAuthenticatedUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof JwtAuthenticationToken jwtAuth) {
            try {
                return UUID.fromString(jwtAuth.getToken().getSubject());
            } catch (IllegalArgumentException e) {
                return null;
            }
        }
        return null;
    }
}
