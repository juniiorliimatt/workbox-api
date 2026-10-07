package br.com.workbox.security.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.cors.CorsConfiguration;

/**
 * As origens liberadas pro browser vêm de {@code cors.allowed-origins} (como no budget-service e no moto-service),
 * não mais fixas no código: o E2E sobe o front em outra porta e precisa liberá-la sem mexer na allowlist de produção.
 */
class CorsConfigurationTest {

    private static CorsConfiguration corsFor(final List<String> origins) {
        final var config = new SecurityConfig(null, null, null, null, null, null, null, null);
        ReflectionTestUtils.setField(config, "allowedOrigins", origins);
        final var request = new MockHttpServletRequest("GET", "/api/v1/auth/login");
        return config.corsConfigurationSource() instanceof org.springframework.web.cors.UrlBasedCorsConfigurationSource source
                ? source.getCorsConfiguration(request)
                : null;
    }

    @Test
    @DisplayName("libera exatamente as origens configuradas, nunca '*' (withCredentials exige origem explícita)")
    void usaAsOrigensConfiguradas() {
        final var cors = corsFor(List.of("http://localhost:5174", "http://127.0.0.1:5174"));

        assertThat(cors).isNotNull();
        assertThat(cors.getAllowedOrigins()).containsExactly("http://localhost:5174", "http://127.0.0.1:5174");
        assertThat(cors.getAllowedOrigins()).doesNotContain("*");
    }

    @Test
    @DisplayName("mantém os métodos permitidos")
    void mantemOsMetodos() {
        final var cors = corsFor(List.of("http://localhost:7053"));

        assertThat(cors.getAllowedMethods()).containsExactlyInAnyOrder("POST", "GET", "PUT", "DELETE", "OPTIONS");
    }
}
