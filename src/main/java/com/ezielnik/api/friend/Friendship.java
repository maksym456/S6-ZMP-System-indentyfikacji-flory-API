package com.ezielnik.api.friend;

import com.ezielnik.api.user.User;
import jakarta.persistence.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "friendships", schema = "public")
public class Friendship {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "requester_id", nullable = false, updatable = false)
    private User requester;

    @ManyToOne(optional = false)
    @JoinColumn(name = "addressee_id", nullable = false, updatable = false)
    private User addressee;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private FriendshipStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    public Friendship() {
    }

    public Friendship(User requester, User addressee) {
        this.requester = requester;
        this.addressee = addressee;
        this.status = FriendshipStatus.PENDING;
    }

    @PrePersist
    public void prePersist() {
        if (id == null) id = UUID.randomUUID();
        if (createdAt == null) createdAt = OffsetDateTime.now();
    }

    public UUID getId() {
        return id;
    }

    public User getRequester() {
        return requester;
    }

    public User getAddressee() {
        return addressee;
    }

    public FriendshipStatus getStatus() {
        return status;
    }

    public void setStatus(FriendshipStatus status) {
        this.status = status;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public User getOtherUser(UUID userId) {
        return requester.getId().equals(userId) ? addressee : requester;
    }
}
