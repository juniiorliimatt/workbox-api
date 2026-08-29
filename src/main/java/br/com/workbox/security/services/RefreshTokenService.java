package br.com.workbox.security.services;

import br.com.workbox.security.entities.RefreshToken;
import br.com.workbox.security.repositories.RefreshTokenRepository;
import java.time.LocalDateTime;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Bookkeeping transacional de refresh tokens (persistência da família/jti), separado do
 * {@link JwtService} de propósito: {@code JwtService} estende {@code OncePerRequestFilter}
 * ({@code GenericFilterBean}), e anotar {@code @Transactional} diretamente nele força o
 * Spring a criar um proxy CGLIB da classe — proxy que, instanciado via Objenesis, nunca
 * roda o inicializador de campo {@code logger} de {@code GenericFilterBean}, e quebra
 * {@code Filter.init(FilterConfig)} com NPE assim que o container (ou o MockMvc em
 * teste) inicializa o filtro. Serviço plano (sem herança de Filter) não tem esse problema.
 */
@Service
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;

    public RefreshTokenService(final RefreshTokenRepository refreshTokenRepository) {
        this.refreshTokenRepository = refreshTokenRepository;
    }

    @Transactional
    public void issue(final UUID userId, final UUID familyId, final UUID jti, final LocalDateTime expiresAt) {
        refreshTokenRepository.save(RefreshToken.builder()
                .id(UUID.randomUUID())
                .jti(jti)
                .familyId(familyId)
                .userId(userId)
                .issuedAt(LocalDateTime.now())
                .expiresAt(expiresAt)
                .build());
    }

    public enum RotationStatus { OK, REUSED, NOT_FOUND }

    public record RotationResult(RotationStatus status, UUID familyId) {
    }

    /**
     * Consome (revoga) o jti apresentado, ou detecta reuso e revoga a família inteira.
     */
    @Transactional
    public RotationResult consume(final UUID jti) {
        final var stored = refreshTokenRepository.findByJti(jti);
        if (stored.isEmpty()) {
            return new RotationResult(RotationStatus.NOT_FOUND, null);
        }

        final var token = stored.get();
        if (token.getRevokedAt() != null) {
            revokeFamily(token.getFamilyId());
            return new RotationResult(RotationStatus.REUSED, token.getFamilyId());
        }

        token.setRevokedAt(LocalDateTime.now());
        refreshTokenRepository.save(token);
        return new RotationResult(RotationStatus.OK, token.getFamilyId());
    }

    private void revokeFamily(final UUID familyId) {
        final var now = LocalDateTime.now();
        final var tokens = refreshTokenRepository.findByFamilyIdAndRevokedAtIsNull(familyId);
        tokens.forEach(t -> t.setRevokedAt(now));
        refreshTokenRepository.saveAll(tokens);
    }
}
