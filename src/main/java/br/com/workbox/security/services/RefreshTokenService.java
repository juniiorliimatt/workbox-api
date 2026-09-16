package br.com.workbox.security.services;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;

/**
 * Bookkeeping de refresh tokens (jti/família/expiração) via Redis em vez de Postgres —
 * TTL nativo por chave substitui o antigo {@code RefreshTokenCleanupJob} (SQL em lote
 * diário), e a detecção de reuso + revogação de família roda atômica num único script
 * Lua ({@code consume_refresh_token.lua}), evitando a corrida que uma leitura+escrita em
 * duas chamadas JPA separadas permitiria sob concorrência.
 *
 * <p>Chaves: {@code refresh:{jti}} (hash: family_id/user_id/revoked, TTL = tempo até
 * expiresAt) e {@code family:{familyId}} (set de jti's da cadeia de rotação, mesma TTL —
 * reaplicada a cada {@link #issue}, então acompanha o último token vivo da família).
 *
 * <p>Separado do {@link JwtService} pelo mesmo motivo de antes (ver histórico): não é um
 * requisito do Redis, é herdado do design original que evitava proxy CGLIB num
 * {@code OncePerRequestFilter}.
 *
 * @author CLAUDE-CODE
 * @author Junior Lima - oojuniiin@outlook.com
 * @since 16/09/2026
 */
@Service
public class RefreshTokenService {

    private static final String REFRESH_KEY_PREFIX = "refresh:";
    private static final String FAMILY_KEY_PREFIX = "family:";

    private final StringRedisTemplate redisTemplate;
    private final RedisScript<List> consumeRefreshTokenScript;

    public RefreshTokenService(final StringRedisTemplate redisTemplate, final RedisScript<List> consumeRefreshTokenScript) {
        this.redisTemplate = redisTemplate;
        this.consumeRefreshTokenScript = consumeRefreshTokenScript;
    }

    public void issue(final UUID userId, final UUID familyId, final UUID jti, final LocalDateTime expiresAt) {
        final var ttl = Duration.between(LocalDateTime.now(), expiresAt);
        final var refreshKey = REFRESH_KEY_PREFIX + jti;
        redisTemplate.opsForHash().putAll(refreshKey, Map.of(
                "family_id", familyId.toString(),
                "user_id", userId.toString(),
                "revoked", "false"));
        redisTemplate.expire(refreshKey, ttl);

        final var familyKey = FAMILY_KEY_PREFIX + familyId;
        redisTemplate.opsForSet().add(familyKey, jti.toString());
        redisTemplate.expire(familyKey, ttl);
    }

    public enum RotationStatus { OK, REUSED, NOT_FOUND }

    public record RotationResult(RotationStatus status, UUID familyId) {
    }

    /**
     * Consome (revoga) o jti apresentado, ou detecta reuso e revoga a família inteira —
     * tudo dentro de {@link #consumeRefreshTokenScript}, atômico.
     */
    @SuppressWarnings("unchecked")
    public RotationResult consume(final UUID jti) {
        final var result = redisTemplate.execute(consumeRefreshTokenScript, List.of(REFRESH_KEY_PREFIX + jti));
        final var status = result.get(0).toString();
        return switch (status) {
            case "not_found" -> new RotationResult(RotationStatus.NOT_FOUND, null);
            case "reused" -> new RotationResult(RotationStatus.REUSED, UUID.fromString(result.get(1).toString()));
            case "ok" -> new RotationResult(RotationStatus.OK, UUID.fromString(result.get(1).toString()));
            default -> throw new IllegalStateException("Unexpected consume_refresh_token.lua result: " + status);
        };
    }
}
