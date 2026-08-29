package br.com.workbox.security.controllers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.workbox.security.dto.MfaCodeDTO;
import br.com.workbox.security.dto.MfaEnrollResponseDTO;
import br.com.workbox.security.dto.MfaLoginDTO;
import br.com.workbox.security.dto.UserApiLoginCredentialsDTO;
import br.com.workbox.security.entities.UserApi;
import br.com.workbox.security.services.JwtService;
import br.com.workbox.security.services.LoginAuditService;
import br.com.workbox.security.services.LoginRateLimiter;
import br.com.workbox.security.services.MfaService;
import br.com.workbox.security.services.PasswordResetService;
import br.com.workbox.security.services.UserApiService;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.core.Authentication;

class AuthControllerTest {

    private JwtService jwtService;
    private UserApiService userApiService;
    private LoginAuditService loginAuditService;
    private LoginRateLimiter loginRateLimiter;
    private PasswordResetService passwordResetService;
    private MfaService mfaService;
    private AuthController controller;

    @BeforeEach
    void setUp() {
        jwtService = mock(JwtService.class);
        userApiService = mock(UserApiService.class);
        loginAuditService = mock(LoginAuditService.class);
        loginRateLimiter = mock(LoginRateLimiter.class);
        passwordResetService = mock(PasswordResetService.class);
        mfaService = mock(MfaService.class);
        controller = new AuthController(jwtService, userApiService, loginAuditService, loginRateLimiter, passwordResetService, mfaService);
        when(loginRateLimiter.isAllowed(anyString())).thenReturn(true);
    }

    private UserApi user(boolean mfaEnabled) {
        return UserApi.builder().id(UUID.randomUUID()).username("alice").password("hash")
                .isEnabled(true).isAccountNonExpired(true).isAccountNonLocked(true).isCredentialsNonExpired(true)
                .tokenVersion(0L).mfaEnabled(mfaEnabled).build();
    }

    @Nested
    @DisplayName("login")
    class Login {

        @Test
        @DisplayName("rate limit excedido responde 429")
        void rateLimited() {
            when(loginRateLimiter.isAllowed(anyString())).thenReturn(false);

            final var response = controller.login(new UserApiLoginCredentialsDTO("alice", "x"), new MockHttpServletRequest());

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        }

        @Test
        @DisplayName("conta com MFA habilitado responde mfa_required em vez dos tokens")
        void mfaRequired() {
            final var user = user(true);
            when(userApiService.attemptLogin("alice", "x")).thenReturn(br.com.workbox.security.services.LoginAttemptResult.success(user));
            when(jwtService.issueMfaChallengeToken(user)).thenReturn("mfa-token");

            final var response = controller.login(new UserApiLoginCredentialsDTO("alice", "x"), new MockHttpServletRequest());

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            @SuppressWarnings("unchecked")
            final var body = (Map<String, Object>) response.getBody();
            assertThat(body.get("mfa_required")).isEqualTo(true);
            assertThat(body.get("mfa_token")).isEqualTo("mfa-token");
        }
    }

    @Nested
    @DisplayName("mfaLogin")
    class MfaLogin {

        @Test
        @DisplayName("rate limit excedido responde 429")
        void rateLimited() {
            when(loginRateLimiter.isAllowed(anyString())).thenReturn(false);

            final var response = controller.mfaLogin(new MfaLoginDTO("tok", "000000"), new MockHttpServletRequest());

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        }

        @Test
        @DisplayName("código inválido responde 401")
        void invalidCode() {
            final var user = user(true);
            when(jwtService.validateMfaChallengeToken("tok")).thenReturn(user);
            when(mfaService.verifyCode(user, "000000")).thenReturn(false);

            final var response = controller.mfaLogin(new MfaLoginDTO("tok", "000000"), new MockHttpServletRequest());

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        }

        @Test
        @DisplayName("código válido emite access+refresh")
        void validCodeIssuesTokens() {
            final var user = user(true);
            when(jwtService.validateMfaChallengeToken("tok")).thenReturn(user);
            when(mfaService.verifyCode(user, "123456")).thenReturn(true);
            when(jwtService.generateToken(user)).thenReturn("access");
            when(jwtService.issueRefreshToken(user)).thenReturn("refresh");

            final var response = controller.mfaLogin(new MfaLoginDTO("tok", "123456"), new MockHttpServletRequest());

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isEqualTo(Map.of("access_token", "access", "refresh_token", "refresh"));
        }
    }

    @Nested
    @DisplayName("refresh")
    class Refresh {

        @Test
        @DisplayName("rate limit excedido responde 429")
        void rateLimited() {
            when(loginRateLimiter.isAllowed(anyString())).thenReturn(false);

            final var response = controller.refresh("token", new MockHttpServletRequest());

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        }
    }

    @Nested
    @DisplayName("forgotPassword")
    class ForgotPassword {

        @Test
        @DisplayName("rate limit excedido responde 429")
        void rateLimited() {
            when(loginRateLimiter.isAllowed(anyString())).thenReturn(false);

            final var response = controller.forgotPassword(new br.com.workbox.security.dto.ForgotPasswordDTO("alice@example.com"));

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        }
    }

    @Nested
    @DisplayName("mfa enroll/verify/disable")
    class MfaManagement {

        @Test
        @DisplayName("enrollMfa devolve o resultado do MfaService pro usuário autenticado")
        void enroll() {
            final var user = user(false);
            final var authentication = mock(Authentication.class);
            when(authentication.getName()).thenReturn("alice");
            when(userApiService.loadUserByUsername("alice")).thenReturn(user);
            final var enrollResult = new MfaEnrollResponseDTO("secret", "otpauth://...");
            when(mfaService.enroll(user)).thenReturn(enrollResult);

            final var response = controller.enrollMfa(authentication);

            assertThat(response.getBody()).isEqualTo(enrollResult);
        }

        @Test
        @DisplayName("verifyMfa delega ao MfaService e responde 204")
        void verifyMfaEndpoint() {
            final var user = user(false);
            final var authentication = mock(Authentication.class);
            when(authentication.getName()).thenReturn("alice");
            when(userApiService.loadUserByUsername("alice")).thenReturn(user);

            final var response = controller.verifyMfa(authentication, new MfaCodeDTO("123456"));

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
            verify(mfaService).verifyAndEnable(user, "123456");
        }

        @Test
        @DisplayName("disableMfa delega ao MfaService e responde 204")
        void disable() {
            final var user = user(true);
            final var authentication = mock(Authentication.class);
            when(authentication.getName()).thenReturn("alice");
            when(userApiService.loadUserByUsername("alice")).thenReturn(user);

            final var response = controller.disableMfa(authentication, new MfaCodeDTO("123456"));

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
            verify(mfaService).disable(user, "123456");
        }
    }
}
