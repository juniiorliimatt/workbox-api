package br.com.workbox.security.services;

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
 */
@Component
public class LoginRateLimiter {

    private static final int MAX_ATTEMPTS = 10;
    private static final Duration WINDOW = Duration.ofMinutes(1);

    private final ConcurrentHashMap<String, Deque<Instant>> attemptsByKey = new ConcurrentHashMap<>();

    public boolean isAllowed(final String key) {
        final var now = Instant.now();
        final var windowStart = now.minus(WINDOW);
        final var deque = attemptsByKey.computeIfAbsent(key, k -> new ConcurrentLinkedDeque<>());

        synchronized (deque) {
            while (!deque.isEmpty() && deque.peekFirst().isBefore(windowStart)) {
                deque.pollFirst();
            }
            if (deque.size() >= MAX_ATTEMPTS) {
                return false;
            }
            deque.addLast(now);
            return true;
        }
    }
}
