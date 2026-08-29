package br.com.workbox.security.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.workbox.security.entities.RefreshToken;
import br.com.workbox.security.repositories.RefreshTokenRepository;
import br.com.workbox.security.services.RefreshTokenService.RotationStatus;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class RefreshTokenServiceTest {

    private RefreshTokenRepository repository;
    private RefreshTokenService service;

    @BeforeEach
    void setUp() {
        repository = mock(RefreshTokenRepository.class);
        service = new RefreshTokenService(repository);
    }

    @Test
    @DisplayName("issue persiste uma nova linha com jti/família/expiração informados")
    void issuePersistsRow() {
        final var userId = UUID.randomUUID();
        final var familyId = UUID.randomUUID();
        final var jti = UUID.randomUUID();
        final var expiresAt = LocalDateTime.now().plusDays(1);

        service.issue(userId, familyId, jti, expiresAt);

        verify(repository).save(argThat(token ->
                token.getUserId().equals(userId)
                        && token.getFamilyId().equals(familyId)
                        && token.getJti().equals(jti)
                        && token.getExpiresAt().equals(expiresAt)
                        && token.getRevokedAt() == null));
    }

    @Nested
    @DisplayName("consume")
    class Consume {

        @Test
        @DisplayName("jti desconhecido retorna NOT_FOUND")
        void notFound() {
            final var jti = UUID.randomUUID();
            when(repository.findByJti(jti)).thenReturn(Optional.empty());

            final var result = service.consume(jti);

            assertThat(result.status()).isEqualTo(RotationStatus.NOT_FOUND);
            assertThat(result.familyId()).isNull();
        }

        @Test
        @DisplayName("jti válido e não revogado retorna OK e marca revokedAt")
        void ok() {
            final var jti = UUID.randomUUID();
            final var familyId = UUID.randomUUID();
            final var token = RefreshToken.builder()
                    .id(UUID.randomUUID()).jti(jti).familyId(familyId).userId(UUID.randomUUID())
                    .issuedAt(LocalDateTime.now()).expiresAt(LocalDateTime.now().plusDays(1))
                    .revokedAt(null)
                    .build();
            when(repository.findByJti(jti)).thenReturn(Optional.of(token));

            final var result = service.consume(jti);

            assertThat(result.status()).isEqualTo(RotationStatus.OK);
            assertThat(result.familyId()).isEqualTo(familyId);
            assertThat(token.getRevokedAt()).isNotNull();
            verify(repository).save(token);
        }

        @Test
        @DisplayName("jti já revogado (reuso) retorna REUSED e revoga toda a família")
        void reusedRevokesFamily() {
            final var jti = UUID.randomUUID();
            final var familyId = UUID.randomUUID();
            final var token = RefreshToken.builder()
                    .id(UUID.randomUUID()).jti(jti).familyId(familyId).userId(UUID.randomUUID())
                    .issuedAt(LocalDateTime.now()).expiresAt(LocalDateTime.now().plusDays(1))
                    .revokedAt(LocalDateTime.now())
                    .build();
            final var sibling = RefreshToken.builder()
                    .id(UUID.randomUUID()).jti(UUID.randomUUID()).familyId(familyId).userId(UUID.randomUUID())
                    .issuedAt(LocalDateTime.now()).expiresAt(LocalDateTime.now().plusDays(1))
                    .revokedAt(null)
                    .build();
            when(repository.findByJti(jti)).thenReturn(Optional.of(token));
            when(repository.findByFamilyIdAndRevokedAtIsNull(familyId)).thenReturn(List.of(sibling));

            final var result = service.consume(jti);

            assertThat(result.status()).isEqualTo(RotationStatus.REUSED);
            assertThat(result.familyId()).isEqualTo(familyId);
            assertThat(sibling.getRevokedAt()).isNotNull();
            verify(repository).saveAll(List.of(sibling));
        }
    }
}
