package br.com.workbox.security.services;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import br.com.workbox.security.repositories.RefreshTokenRepository;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class RefreshTokenCleanupJobTest {

    @Test
    void purgeExpiredDeletesRowsOlderThanNow() {
        final var repository = mock(RefreshTokenRepository.class);
        final var job = new RefreshTokenCleanupJob(repository);

        job.purgeExpired();

        verify(repository).deleteByExpiresAtBefore(any(LocalDateTime.class));
    }
}
