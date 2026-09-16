package br.com.workbox;

import br.com.workbox.steps.MailTestConfig;
import io.cucumber.spring.CucumberContextConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Cenários de autenticação/MFA/reset de senha exercitam login de verdade, que agora
 * emite refresh token via {@code RefreshTokenService} (Redis) — sem um Redis real
 * alcançável aqui, todo cenário que passa por login falha com erro de conexão. Container
 * único, compartilhado por todos os cenários desta JVM.
 *
 * <p>Sem {@code @Testcontainers}/{@code @Container}: essas anotações só funcionam quando
 * o JUnit 5 processa a classe diretamente como classe de teste — Cucumber nunca invoca
 * extensões JUnit 5 sobre {@code CucumberSpringConfiguration} (só lê
 * {@code @CucumberContextConfiguration} via reflection), então o container nunca seria
 * iniciado a tempo. Lifecycle gerenciado manualmente no bloco estático.
 */
@CucumberContextConfiguration
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Import(MailTestConfig.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
public class CucumberSpringConfiguration {

    static final GenericContainer<?> REDIS = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379);

    static {
        REDIS.start();
    }

    @DynamicPropertySource
    static void redisProperties(final DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", REDIS::getHost);
        registry.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));
    }
}
