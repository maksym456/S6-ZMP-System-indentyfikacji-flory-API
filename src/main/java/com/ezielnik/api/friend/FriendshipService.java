package com.ezielnik.api.friend;

import com.ezielnik.api.notification.NotificationService;
import com.ezielnik.api.user.User;
import com.ezielnik.api.user.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Service
public class FriendshipService {

    private final FriendshipRepository friendshipRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    public FriendshipService(FriendshipRepository friendshipRepository,
                             UserRepository userRepository,
                             NotificationService notificationService) {
        this.friendshipRepository = friendshipRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
    }

    @Transactional
    public FriendResponse sendRequest(UUID requesterId, String targetUsername) {
        if (targetUsername == null || targetUsername.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Username is required");
        }

        User requester = userRepository.findById(requesterId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        User addressee = userRepository.findByUsername(targetUsername)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        if (requester.getId().equals(addressee.getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "You cannot add yourself as a friend");
        }

        friendshipRepository.findBetween(requesterId, addressee.getId()).ifPresent(existing -> {
            if (existing.getStatus() == FriendshipStatus.ACCEPTED) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "You are already friends");
            }
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A friend request already exists");
        });

        Friendship friendship = friendshipRepository.save(new Friendship(requester, addressee));

        notificationService.createNotification(
                addressee,
                "Friend request",
                requester.getUsername() + " sent you a friend request"
        );

        return new FriendResponse(friendship, requesterId);
    }

    @Transactional
    public FriendResponse acceptRequest(UUID userId, UUID friendshipId) {
        Friendship friendship = friendshipRepository.findById(friendshipId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Friend request not found"));

        if (!friendship.getAddressee().getId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You cannot accept this request");
        }

        if (friendship.getStatus() != FriendshipStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This request is no longer pending");
        }

        friendship.setStatus(FriendshipStatus.ACCEPTED);
        friendshipRepository.save(friendship);

        notificationService.createNotification(
                friendship.getRequester(),
                "Friend request accepted",
                friendship.getAddressee().getUsername() + " accepted your friend request"
        );

        return new FriendResponse(friendship, userId);
    }

    @SuppressWarnings("SameReturnValue")
    @Transactional
    public String removeFriendship(UUID userId, UUID friendshipId) {
        Friendship friendship = friendshipRepository.findById(friendshipId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Friendship not found"));

        boolean isRequester = friendship.getRequester().getId().equals(userId);
        boolean isAddressee = friendship.getAddressee().getId().equals(userId);

        if (!isRequester && !isAddressee) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not part of this friendship");
        }

        friendshipRepository.delete(friendship);
        return "Friendship removed";
    }

    @Transactional(readOnly = true)
    public List<FriendResponse> getFriends(UUID userId) {
        return friendshipRepository.findAcceptedByUserId(userId)
                .stream()
                .map(f -> new FriendResponse(f, userId))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<FriendResponse> getIncomingRequests(UUID userId) {
        return friendshipRepository.findByAddressee_IdAndStatus(userId, FriendshipStatus.PENDING)
                .stream()
                .map(f -> new FriendResponse(f, userId))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<FriendResponse> getSentRequests(UUID userId) {
        return friendshipRepository.findByRequester_IdAndStatus(userId, FriendshipStatus.PENDING)
                .stream()
                .map(f -> new FriendResponse(f, userId))
                .toList();
    }
}
