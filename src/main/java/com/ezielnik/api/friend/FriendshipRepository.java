package com.ezielnik.api.friend;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FriendshipRepository extends JpaRepository<Friendship, UUID> {

    @Query("SELECT f FROM Friendship f WHERE (f.requester.id = :a AND f.addressee.id = :b) OR (f.requester.id = :b AND f.addressee.id = :a)")
    Optional<Friendship> findBetween(@Param("a") UUID a, @Param("b") UUID b);

    @Query("SELECT f FROM Friendship f WHERE f.status = 'ACCEPTED' AND (f.requester.id = :userId OR f.addressee.id = :userId)")
    List<Friendship> findAcceptedByUserId(@Param("userId") UUID userId);

    List<Friendship> findByAddressee_IdAndStatus(UUID addresseeId, FriendshipStatus status);

    List<Friendship> findByRequester_IdAndStatus(UUID requesterId, FriendshipStatus status);

    @Query("SELECT CASE WHEN COUNT(f) > 0 THEN true ELSE false END FROM Friendship f WHERE f.status = 'ACCEPTED' AND ((f.requester.id = :a AND f.addressee.id = :b) OR (f.requester.id = :b AND f.addressee.id = :a))")
    boolean areFriends(@Param("a") UUID a, @Param("b") UUID b);

    long countByStatus(FriendshipStatus status);

    @Query("SELECT COUNT(f) FROM Friendship f WHERE f.status = 'ACCEPTED' AND (f.requester.id = :userId OR f.addressee.id = :userId)")
    long countAcceptedByUserId(@Param("userId") UUID userId);
}
