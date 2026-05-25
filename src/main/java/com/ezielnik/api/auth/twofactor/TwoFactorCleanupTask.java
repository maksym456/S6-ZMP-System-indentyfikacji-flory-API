package com.ezielnik.api.auth.twofactor;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

@Component
public class TwoFactorCleanupTask {

    private final TwoFactorCodeRepository codeRepository;

    public TwoFactorCleanupTask(TwoFactorCodeRepository codeRepository) {
        this.codeRepository = codeRepository;
    }

    @Scheduled(fixedRate = 3600000)
    @Transactional
    public void deleteExpiredCodes() {
        codeRepository.deleteExpiredBefore(OffsetDateTime.now().minusHours(1));
    }
}
