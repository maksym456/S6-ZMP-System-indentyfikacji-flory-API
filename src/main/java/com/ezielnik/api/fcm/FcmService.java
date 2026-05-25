package com.ezielnik.api.fcm;

import com.ezielnik.api.user.User;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class FcmService {

    private static final Logger log = LoggerFactory.getLogger(FcmService.class);

    private final FirebaseMessaging messaging;
    private final DeviceTokenRepository deviceTokenRepository;

    public FcmService(@Value("${FIREBASE_SERVICE_ACCOUNT_JSON:}") String serviceAccountJson,
                      DeviceTokenRepository deviceTokenRepository) {
        this.deviceTokenRepository = deviceTokenRepository;

        FirebaseMessaging m = null;
        if (serviceAccountJson != null && !serviceAccountJson.isBlank()) {
            try {
                GoogleCredentials credentials = GoogleCredentials.fromStream(
                        new ByteArrayInputStream(serviceAccountJson.getBytes(StandardCharsets.UTF_8))
                );
                FirebaseOptions options = FirebaseOptions.builder()
                        .setCredentials(credentials)
                        .build();
                if (FirebaseApp.getApps().isEmpty()) {
                    FirebaseApp.initializeApp(options);
                }
                m = FirebaseMessaging.getInstance();
                log.info("Firebase initialized successfully");
            } catch (IOException e) {
                log.error("Failed to initialize Firebase: {}", e.getMessage());
            }
        } else {
            log.warn("FIREBASE_SERVICE_ACCOUNT_JSON not set — push notifications disabled");
        }
        this.messaging = m;
    }

    public void sendToUser(User user, String title, String body) {
        if (messaging == null) return;

        List<DeviceToken> tokens = deviceTokenRepository.findByUserId(user.getId());
        if (tokens.isEmpty()) return;

        List<String> tokenStrings = tokens.stream().map(DeviceToken::getToken).toList();

        MulticastMessage message = MulticastMessage.builder()
                .setNotification(Notification.builder()
                        .setTitle(title)
                        .setBody(body)
                        .build())
                .addAllTokens(tokenStrings)
                .build();

        try {
            BatchResponse response = messaging.sendEachForMulticast(message);

            if (response.getFailureCount() > 0) {
                List<SendResponse> responses = response.getResponses();
                List<DeviceToken> stale = new ArrayList<>();
                for (int i = 0; i < responses.size(); i++) {
                    SendResponse r = responses.get(i);
                    if (!r.isSuccessful()) {
                        FirebaseMessagingException ex = r.getException();
                        if (ex != null && ex.getMessagingErrorCode() == MessagingErrorCode.UNREGISTERED) {
                            stale.add(tokens.get(i));
                        }
                    }
                }
                if (!stale.isEmpty()) {
                    deviceTokenRepository.deleteAll(stale);
                    log.info("Removed {} stale FCM token(s) for user {}", stale.size(), user.getId());
                }
            }
        } catch (FirebaseMessagingException e) {
            log.error("FCM send failed for user {}: {}", user.getId(), e.getMessage());
        }
    }

    @Transactional
    public void registerToken(UUID userId, String token, User user) {
        if (token == null || token.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Token is required");
        }
        deviceTokenRepository.findByToken(token).ifPresentOrElse(
                existing -> {
                    if (!existing.getUser().getId().equals(userId)) {
                        existing.setUser(user);
                        deviceTokenRepository.save(existing);
                    }
                },
                () -> deviceTokenRepository.save(new DeviceToken(user, token))
        );
    }

    @Transactional
    public void unregisterToken(UUID userId, String token) {
        if (token == null || token.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Token is required");
        }
        deviceTokenRepository.deleteByUserIdAndToken(userId, token);
    }
}
