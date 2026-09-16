package br.com.workbox.security.services;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/**
 * Redis real e descartável via Testcontainers — testa o script Lua de fato, não uma
 * simulação em memória. {@link RefreshTokenService} é instanciado direto (sem
 * {@code @SpringBootTest}): seu construtor só depende de {@code StringRedisTemplate} +
 * {@code RedisScript}, nada de contexto Spring é necessário pra exercitar a lógica real.
 */
@Testcontainers
class RefreshTokenServiceIT {

    @Container
    static final GenericContainer<?> REDIS = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379);

    private RefreshTokenService service;
    private LettuceConnectionFactory connectionFactory;

    @BeforeEach
    void setUp() {
        final var config = new RedisStandaloneConfiguration(REDIS.getHost(), REDIS.getMappedPort(6379));
        connectionFactory = new LettuceConnectionFactory(config);
        connectionFactory.afterPropertiesSet();

        final var redisTemplate = new StringRedisTemplate(connectionFactory);
        redisTemplate.afterPropertiesSet();

        final var script = new DefaultRedisScript<List>();
        script.setLocation(new ClassPathResource("scripts/consume_refresh_token.lua"));
        script.setResultType(List.class);

        service = new RefreshTokenService(redisTemplate, script);
    }

    @AfterEach
    void tearDown() {
        connectionFactory.destroy();
    }

    @Nested
    @DisplayName("consume")
    class Consume {

        @Test
        @DisplayName("jti desconhecido retorna NOT_FOUND")
        void notFound() {
            final var result = service.consume(UUID.randomUUID());

            assertThat(result.status()).isEqualTo(RefreshTokenService.RotationStatus.NOT_FOUND);
            assertThat(result.familyId()).isNull();
        }

        @Test
        @DisplayName("jti válido e não consumido ainda retorna OK")
        void ok() {
            final var userId = UUID.randomUUID();
            final var familyId = UUID.randomUUID();
            final var jti = UUID.randomUUID();
            service.issue(userId, familyId, jti, LocalDateTime.now().plusDays(1));

            final var result = service.consume(jti);

            assertThat(result.status()).isEqualTo(RefreshTokenService.RotationStatus.OK);
            assertThat(result.familyId()).isEqualTo(familyId);
        }

        @Test
        @DisplayName("consumir o mesmo jti duas vezes detecta reuso na segunda")
        void secondConsumeIsReused() {
            final var userId = UUID.randomUUID();
            final var familyId = UUID.randomUUID();
            final var jti = UUID.randomUUID();
            service.issue(userId, familyId, jti, LocalDateTime.now().plusDays(1));

            service.consume(jti);
            final var result = service.consume(jti);

            assertThat(result.status()).isEqualTo(RefreshTokenService.RotationStatus.REUSED);
            assertThat(result.familyId()).isEqualTo(familyId);
        }

        @Test
        @DisplayName("reuso revoga a família inteira — jti irmão vivo vira NOT_FOUND na sequência")
        void reuseRevokesWholeFamily() {
            final var userId = UUID.randomUUID();
            final var familyId = UUID.randomUUID();
            final var firstJti = UUID.randomUUID();
            final var secondJti = UUID.randomUUID();
            service.issue(userId, familyId, firstJti, LocalDateTime.now().plusDays(1));
            service.consume(firstJti);
            service.issue(userId, familyId, secondJti, LocalDateTime.now().plusDays(1));

            // Reapresenta o primeiro jti (já consumido) — reuso, revoga a família toda,
            // incluindo o secondJti que ainda estava válido.
            service.consume(firstJti);
            final var result = service.consume(secondJti);

            assertThat(result.status()).isEqualTo(RefreshTokenService.RotationStatus.NOT_FOUND);
        }

        @Test
        @DisplayName("issue aplica TTL — chave expira sozinha sem job de cleanup")
        void issueAppliesNativeTtl() throws InterruptedException {
            final var jti = UUID.randomUUID();
            service.issue(UUID.randomUUID(), UUID.randomUUID(), jti, LocalDateTime.now().plusSeconds(1));

            Thread.sleep(1500);

            assertThat(service.consume(jti).status()).isEqualTo(RefreshTokenService.RotationStatus.NOT_FOUND);
        }
    }
}
