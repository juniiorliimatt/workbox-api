package br.com.workbox.security.services;

import br.com.workbox.exceptions.InvalidTokenException;
import br.com.workbox.exceptions.LoginInvalidException;
import br.com.workbox.security.dto.MfaEnrollResponseDTO;
import br.com.workbox.security.entities.UserApi;
import br.com.workbox.security.repositories.UserApiRepository;
import dev.samstevens.totp.code.CodeVerifier;
import dev.samstevens.totp.code.DefaultCodeGenerator;
import dev.samstevens.totp.code.DefaultCodeVerifier;
import dev.samstevens.totp.qr.QrData;
import dev.samstevens.totp.secret.DefaultSecretGenerator;
import dev.samstevens.totp.secret.SecretGenerator;
import dev.samstevens.totp.time.SystemTimeProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * TOTP (RFC 6238) como segundo fator, complementar ao login por senha. O segredo só é
 * confirmado ({@code mfaEnabled=true}) depois de {@link #verifyAndEnable} validar o
 * primeiro código — evita habilitar MFA com um segredo que o usuário nunca conseguiu
 * escanear/configurar corretamente no app autenticador.
 *
 * @author CLAUDE-CODE
 * @author Junior Lima - oojuniin@outlook.com
 * @since 29-08-2026
 */
@Service
public class MfaService {

    private static final String ISSUER = "workbox-api";

    private final UserApiRepository userApiRepository;
    private final SecretGenerator secretGenerator = new DefaultSecretGenerator();
    private final CodeVerifier codeVerifier = new DefaultCodeVerifier(new DefaultCodeGenerator(), new SystemTimeProvider());

    public MfaService(final UserApiRepository userApiRepository) {
        this.userApiRepository = userApiRepository;
    }

    @Transactional
    public MfaEnrollResponseDTO enroll(final UserApi user) {
        final var secret = secretGenerator.generate();
        user.setMfaSecret(secret);
        user.setMfaEnabled(false);
        userApiRepository.save(user);

        final var otpAuthUri = new QrData.Builder()
                .label(user.getUsername())
                .secret(secret)
                .issuer(ISSUER)
                .build()
                .getUri();
        return new MfaEnrollResponseDTO(secret, otpAuthUri);
    }

    @Transactional
    public void verifyAndEnable(final UserApi user, final String code) {
        if (user.getMfaSecret() == null || !codeVerifier.isValidCode(user.getMfaSecret(), code)) {
            throw new InvalidTokenException("Invalid MFA code");
        }
        user.setMfaEnabled(true);
        userApiRepository.save(user);
    }

    @Transactional
    public void disable(final UserApi user, final String code) {
        if (!Boolean.TRUE.equals(user.getMfaEnabled()) || !codeVerifier.isValidCode(user.getMfaSecret(), code)) {
            throw new LoginInvalidException("Invalid MFA code");
        }
        user.setMfaSecret(null);
        user.setMfaEnabled(false);
        userApiRepository.save(user);
    }

    public boolean verifyCode(final UserApi user, final String code) {
        return user.getMfaSecret() != null && codeVerifier.isValidCode(user.getMfaSecret(), code);
    }
}
