package br.com.workbox.security.services;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Deque;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * Sliding window em memória, por IP — suficiente pra uma instância única (não escala
 * horizontalmente; múltiplas instâncias precisariam de um contador compartilhado, ex.
 * Redis). Complementa o lockout por conta em {@link UserApiService#attemptLogin}: esse
 * aqui pega o atacante testando várias contas do mesmo IP.
 *
 * Limite configurável (não só um {@code final int}) porque os cenários de Cucumber
 * rodam dezenas de logins no mesmo "IP" (MockMvc) dentro da mesma janela — sem isso o
 * limite de produção deixaria os testes flaky por ordem de execução. Ver
 * application-test.properties.
 */
@Component
public class LoginRateLimiter {

    private final int maxAttempts;
    private final Duration window;

    private final ConcurrentHashMap<String, Deque<Instant>> attemptsByKey = new ConcurrentHashMap<>();

    public LoginRateLimiter(@Value("${login.rate-limit.max-attempts:10}") final int maxAttempts,
                             @Value("${login.rate-limit.window-seconds:60}") final long windowSeconds) {
        this.maxAttempts = maxAttempts;
        this.window = Duration.ofSeconds(windowSeconds);
    }

    public boolean isAllowed(final String key) {
        final var now = Instant.now();
        final var windowStart = now.minus(window);
        final var deque = attemptsByKey.computeIfAbsent(key, k -> new ConcurrentLinkedDeque<>());

        synchronized (deque) {
            while (!deque.isEmpty() && deque.peekFirst().isBefore(windowStart)) {
                deque.pollFirst();
            }
            if (deque.size() >= maxAttempts) {
                return false;
            }
            deque.addLast(now);
            return true;
        }
    }
}
