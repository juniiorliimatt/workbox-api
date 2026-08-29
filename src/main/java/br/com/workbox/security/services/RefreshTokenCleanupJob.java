package br.com.workbox.security.services;

import br.com.workbox.security.repositories.RefreshTokenRepository;
import java.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Sem isso, {@code refresh_tokens} cresce sem limite (uma linha por login/rotação, nunca
 * apagada) — expurga diariamente o que já expirou, revogado ou não.
 *
 * @author CLAUDE-CODE
 * @author Junior Lima - oojuniin@outlook.com
 * @since 29-08-2026
 */
@Component
public class RefreshTokenCleanupJob {

    private static final Logger logger = LoggerFactory.getLogger(RefreshTokenCleanupJob.class);

    private final RefreshTokenRepository refreshTokenRepository;

    public RefreshTokenCleanupJob(final RefreshTokenRepository refreshTokenRepository) {
        this.refreshTokenRepository = refreshTokenRepository;
    }

    @Scheduled(cron = "0 0 3 * * *")
    public void purgeExpired() {
        logger.info("Purging expired refresh tokens");
        refreshTokenRepository.deleteByExpiresAtBefore(LocalDateTime.now());
    }
}
