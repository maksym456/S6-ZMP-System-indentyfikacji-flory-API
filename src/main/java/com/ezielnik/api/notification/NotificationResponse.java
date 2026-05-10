package com.ezielnik.api.notification;

import java.time.OffsetDateTime;
import java.util.UUID;

public class NotificationResponse {

    private final UUID id;
    private final String title;
    private final String message;
    private final boolean read;
    private final OffsetDateTime createdAt;

    public NotificationResponse(Notification notification) {
        this.id = notification.getId();
        this.title = notification.getTitle();
        this.message = notification.getMessage();
        this.read = notification.isRead();
        this.createdAt = notification.getCreatedAt();
    }

    public UUID getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getMessage() {
        return message;
    }

    public boolean isRead() {
        return read;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
}