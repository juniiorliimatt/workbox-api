package br.com.workbox.security.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import br.com.workbox.exceptions.InvalidTokenException;
import br.com.workbox.exceptions.LoginInvalidException;
import br.com.workbox.security.entities.UserApi;
import br.com.workbox.security.repositories.UserApiRepository;
import dev.samstevens.totp.code.DefaultCodeGenerator;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class MfaServiceTest {

    private static final int TIME_PERIOD_SECONDS = 30;

    private UserApiRepository userApiRepository;
    private MfaService mfaService;
    private final DefaultCodeGenerator codeGenerator = new DefaultCodeGenerator();

    @BeforeEach
    void setUp() {
        userApiRepository = mock(UserApiRepository.class);
        mfaService = new MfaService(userApiRepository);
    }

    private UserApi user() {
        return UserApi.builder().id(UUID.randomUUID()).username("alice").password("hash").build();
    }

    private String currentCode(String secret) throws Exception {
        return codeGenerator.generate(secret, Instant.now().getEpochSecond() / TIME_PERIOD_SECONDS);
    }

    @Test
    @DisplayName("enroll gera um segredo novo, não habilita MFA ainda, e devolve a otpauth URI")
    void enrollGeneratesSecretWithoutEnabling() {
        final var user = user();

        final var result = mfaService.enroll(user);

        assertThat(result.secret()).isNotBlank();
        assertThat(result.otpAuthUri()).startsWith("otpauth://totp/alice?secret=" + result.secret());
        assertThat(user.getMfaSecret()).isEqualTo(result.secret());
        assertThat(user.getMfaEnabled()).isFalse();
        verify(userApiRepository).save(user);
    }

    @Nested
    @DisplayName("verifyAndEnable")
    class VerifyAndEnable {

        @Test
        @DisplayName("código correto habilita MFA")
        void correctCodeEnables() throws Exception {
            final var user = user();
            final var secret = mfaService.enroll(user).secret();

            mfaService.verifyAndEnable(user, currentCode(secret));

            assertThat(user.getMfaEnabled()).isTrue();
        }

        @Test
        @DisplayName("código incorreto lança InvalidTokenException e não habilita")
        void wrongCodeThrows() {
            final var user = user();
            mfaService.enroll(user);

            assertThatThrownBy(() -> mfaService.verifyAndEnable(user, "000000"))
                    .isInstanceOf(InvalidTokenException.class);
            assertThat(user.getMfaEnabled()).isFalse();
        }

        @Test
        @DisplayName("sem segredo (nunca fez enroll) lança InvalidTokenException")
        void noSecretThrows() {
            final var user = user();

            assertThatThrownBy(() -> mfaService.verifyAndEnable(user, "000000"))
                    .isInstanceOf(InvalidTokenException.class);
        }
    }

    @Nested
    @DisplayName("disable")
    class Disable {

        @Test
        @DisplayName("código correto desabilita e limpa o segredo")
        void correctCodeDisables() throws Exception {
            final var user = user();
            final var secret = mfaService.enroll(user).secret();
            mfaService.verifyAndEnable(user, currentCode(secret));

            mfaService.disable(user, currentCode(secret));

            assertThat(user.getMfaEnabled()).isFalse();
            assertThat(user.getMfaSecret()).isNull();
        }

        @Test
        @DisplayName("MFA não habilitado lança LoginInvalidException")
        void notEnabledThrows() {
            final var user = user();

            assertThatThrownBy(() -> mfaService.disable(user, "000000"))
                    .isInstanceOf(LoginInvalidException.class);
        }

        @Test
        @DisplayName("código incorreto com MFA habilitado lança LoginInvalidException")
        void wrongCodeThrows() throws Exception {
            final var user = user();
            final var secret = mfaService.enroll(user).secret();
            mfaService.verifyAndEnable(user, currentCode(secret));

            assertThatThrownBy(() -> mfaService.disable(user, "000000"))
                    .isInstanceOf(LoginInvalidException.class);
            assertThat(user.getMfaEnabled()).isTrue();
        }
    }

    @Nested
    @DisplayName("verifyCode")
    class VerifyCode {

        @Test
        @DisplayName("sem segredo retorna false")
        void noSecretReturnsFalse() {
            assertThat(mfaService.verifyCode(user(), "000000")).isFalse();
        }

        @Test
        @DisplayName("código correto retorna true")
        void correctCodeReturnsTrue() throws Exception {
            final var user = user();
            final var secret = mfaService.enroll(user).secret();

            assertThat(mfaService.verifyCode(user, currentCode(secret))).isTrue();
        }
    }
}
