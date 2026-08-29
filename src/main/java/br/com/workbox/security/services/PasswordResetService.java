package br.com.workbox.security.services;

import br.com.workbox.exceptions.InvalidTokenException;
import br.com.workbox.security.entities.PasswordResetToken;
import br.com.workbox.security.repositories.PasswordResetTokenRepository;
import br.com.workbox.security.repositories.UserApiRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author CLAUDE-CODE
 * @author Junior Lima - oojuniin@outlook.com
 * @since 29-08-2026
 */

@Service
public class PasswordResetService {

    private static final int TOKEN_BYTES = 32;
    private static final Duration TOKEN_TTL = Duration.ofMinutes(30);
    private static final String INVALID_OR_EXPIRED = "Invalid or expired reset token";

    private final PasswordResetTokenRepository tokenRepository;
    private final UserApiRepository userApiRepository;
    private final PasswordEncoder passwordEncoder;
    private final MailService mailService;
    private final SecureRandom secureRandom = new SecureRandom();

    public PasswordResetService(final PasswordResetTokenRepository tokenRepository,
                                 final UserApiRepository userApiRepository,
                                 final PasswordEncoder passwordEncoder,
                                 final MailService mailService) {
        this.tokenRepository = tokenRepository;
        this.userApiRepository = userApiRepository;
        this.passwordEncoder = passwordEncoder;
        this.mailService = mailService;
    }

    /**
     * Não revela se o e-mail existe — se não existir, simplesmente não faz nada, mas
     * responde da mesma forma pro chamador (ver AuthController).
     */
    @Transactional
    public void requestReset(final String email) {
        userApiRepository.findByEmail(email).ifPresent(user -> {
            final var rawToken = generateRawToken();
            tokenRepository.save(PasswordResetToken.builder()
                    .id(UUID.randomUUID())
                    .user(user)
                    .tokenHash(hash(rawToken))
                    .expiresAt(LocalDateTime.now().plus(TOKEN_TTL))
                    .used(false)
                    .createdAt(LocalDateTime.now())
                    .build());
            mailService.sendPasswordResetEmail(email, rawToken);
        });
    }

    @Transactional
    public void resetPassword(final String rawToken, final String newPassword) {
        final var token = tokenRepository.findByTokenHash(hash(rawToken))
                .orElseThrow(() -> new InvalidTokenException(INVALID_OR_EXPIRED));
        if (token.isUsed() || token.isExpired()) {
            throw new InvalidTokenException(INVALID_OR_EXPIRED);
        }

        final var user = token.getUser();
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setTokenVersion(user.getTokenVersion() + 1);
        userApiRepository.save(user);

        token.setUsed(true);
        tokenRepository.save(token);
    }

    private String generateRawToken() {
        final var bytes = new byte[TOKEN_BYTES];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hash(final String raw) {
        try {
            final var digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(raw.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }
}
