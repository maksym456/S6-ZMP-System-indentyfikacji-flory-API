package com.ezielnik.api.auth.twofactor;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

public interface TwoFactorCodeRepository extends JpaRepository<TwoFactorCode, UUID> {

    Optional<TwoFactorCode> findTopByUser_IdAndUsedFalseAndExpiresAtAfterOrderByExpiresAtDesc(UUID userId, OffsetDateTime now);

    @Modifying
    @Query("UPDATE TwoFactorCode c SET c.used = true WHERE c.user.id = :userId AND c.used = false")
    void invalidateAllForUser(@Param("userId") UUID userId);

    @Modifying
    @Query("DELETE FROM TwoFactorCode c WHERE c.expiresAt < :cutoff")
    void deleteExpiredBefore(@Param("cutoff") OffsetDateTime cutoff);
}
