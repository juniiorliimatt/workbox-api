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
import br.com.workbox.security.dto.UserApiRegisterDTO;
import br.com.workbox.security.entities.UserApi;
import br.com.workbox.security.services.AvatarService;
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
    private AvatarService avatarService;
    private AuthController controller;

    @BeforeEach
    void setUp() {
        jwtService = mock(JwtService.class);
        userApiService = mock(UserApiService.class);
        loginAuditService = mock(LoginAuditService.class);
        loginRateLimiter = mock(LoginRateLimiter.class);
        passwordResetService = mock(PasswordResetService.class);
        mfaService = mock(MfaService.class);
        avatarService = mock(AvatarService.class);
        controller = new AuthController(jwtService, userApiService, loginAuditService, loginRateLimiter, passwordResetService, mfaService, avatarService);
        when(loginRateLimiter.isAllowed(anyString())).thenReturn(true);
    }

    private UserApi user(boolean mfaEnabled) {
        return UserApi.builder().id(UUID.randomUUID()).socialName("Alice").email("alice@example.com").password("hash")
                .isEnabled(true).isAccountNonExpired(true).isAccountNonLocked(true).isCredentialsNonExpired(true)
                .tokenVersion(0L).mfaEnabled(mfaEnabled).build();
    }

    @Nested
    @DisplayName("register")
    class Register {

        @Test
        @DisplayName("rate limit excedido responde 429")
        void rateLimited() {
            when(loginRateLimiter.isAllowed(anyString())).thenReturn(false);

            final var response = controller.register(new UserApiRegisterDTO("alice", "alice@example.com", "S3nh@Forte!"), new MockHttpServletRequest());

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        }

        @Test
        @DisplayName("cadastro válido delega ao UserApiService e responde 201")
        void createsUser() {
            final var dto = new UserApiRegisterDTO("alice", "alice@example.com", "S3nh@Forte!");
            final var created = new br.com.workbox.security.dto.UserApiDTO(UUID.randomUUID(), "alice", "alice@example.com", true, null);
            when(userApiService.register(dto)).thenReturn(created);

            final var response = controller.register(dto, new MockHttpServletRequest());

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
            assertThat(response.getBody()).isEqualTo(created);
        }
    }

    @Nested
    @DisplayName("login")
    class Login {

        @Test
        @DisplayName("rate limit excedido responde 429")
        void rateLimited() {
            when(loginRateLimiter.isAllowed(anyString())).thenReturn(false);

            final var response = controller.login(new UserApiLoginCredentialsDTO("alice@example.com", "x"), new MockHttpServletRequest());

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        }

        @Test
        @DisplayName("conta com MFA habilitado responde mfa_required em vez dos tokens")
        void mfaRequired() {
            final var user = user(true);
            when(userApiService.attemptLogin("alice@example.com", "x")).thenReturn(br.com.workbox.security.services.LoginAttemptResult.success(user));
            when(jwtService.issueMfaChallengeToken(user)).thenReturn("mfa-token");

            final var response = controller.login(new UserApiLoginCredentialsDTO("alice@example.com", "x"), new MockHttpServletRequest());

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
    @DisplayName("avatar")
    class Avatar {

        @Test
        @DisplayName("uploadAvatar delega ao AvatarService com o id do usuário autenticado")
        void upload() {
            final var user = user(false);
            final var authentication = mock(Authentication.class);
            when(authentication.getName()).thenReturn("alice@example.com");
            when(userApiService.loadUserByUsername("alice@example.com")).thenReturn(user);
            final var file = new org.springframework.mock.web.MockMultipartFile("file", "avatar.png", "image/png", new byte[]{1, 2, 3});

            final var response = controller.uploadAvatar(authentication, file);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
            verify(avatarService).store(user.getId(), file);
        }

        @Test
        @DisplayName("deleteAvatar delega ao AvatarService com o id do usuário autenticado")
        void delete() {
            final var user = user(false);
            final var authentication = mock(Authentication.class);
            when(authentication.getName()).thenReturn("alice@example.com");
            when(userApiService.loadUserByUsername("alice@example.com")).thenReturn(user);

            final var response = controller.deleteAvatar(authentication);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
            verify(avatarService).delete(user.getId());
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
            when(authentication.getName()).thenReturn("alice@example.com");
            when(userApiService.loadUserByUsername("alice@example.com")).thenReturn(user);
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
            when(authentication.getName()).thenReturn("alice@example.com");
            when(userApiService.loadUserByUsername("alice@example.com")).thenReturn(user);

            final var response = controller.verifyMfa(authentication, new MfaCodeDTO("123456"));

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
            verify(mfaService).verifyAndEnable(user, "123456");
        }

        @Test
        @DisplayName("disableMfa delega ao MfaService e responde 204")
        void disable() {
            final var user = user(true);
            final var authentication = mock(Authentication.class);
            when(authentication.getName()).thenReturn("alice@example.com");
            when(userApiService.loadUserByUsername("alice@example.com")).thenReturn(user);

            final var response = controller.disableMfa(authentication, new MfaCodeDTO("123456"));

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
            verify(mfaService).disable(user, "123456");
        }
    }
}
