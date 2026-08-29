package br.com.workbox.security.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.workbox.exceptions.InvalidRefreshTokenException;
import br.com.workbox.security.entities.Role;
import br.com.workbox.security.entities.UserApi;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import jakarta.servlet.FilterChain;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

class JwtServiceTest {

    private static final SecretKey SECRET_KEY =
            new SecretKeySpec("MyS3cur3P@ssw0rd12345!ThisIs32Bytes!".getBytes(), "HmacSHA256");

    private UserApiService userApiService;
    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        userApiService = mock(UserApiService.class);
        jwtService = new JwtService(SECRET_KEY, userApiService);
    }

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    private UserApi enabledUser(String username) {
        return UserApi.builder()
                .id(UUID.randomUUID())
                .username(username)
                .password("hash")
                .isEnabled(true)
                .isAccountNonExpired(true)
                .isAccountNonLocked(true)
                .isCredentialsNonExpired(true)
                .tokenVersion(0L)
                .roles(Set.of(Role.builder().id(1L).authority("USER").build()))
                .build();
    }

    private String rawToken(String typ, String subject, long validityMs, long tokenVersion) {
        final var claims = new HashMap<String, Object>();
        claims.put("typ", typ);
        claims.put("tv", tokenVersion);
        claims.put("roles", List.of("ROLE_USER"));
        final var now = new Date();
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(subject)
                .setIssuedAt(now)
                .setExpiration(new Date(now.getTime() + validityMs))
                .signWith(SECRET_KEY, SignatureAlgorithm.HS256)
                .compact();
    }

    @Nested
    @DisplayName("generateToken / generateRefreshToken")
    class Generation {

        @Test
        @DisplayName("access token carrega typ=access, subject, tv e roles prefixadas")
        void accessTokenClaims() {
            final var user = enabledUser("alice");
            final var token = jwtService.generateToken(user);

            final var claims = Jwts.parserBuilder().setSigningKey(SECRET_KEY).build()
                    .parseClaimsJws(token).getBody();

            assertThat(claims.getSubject()).isEqualTo("alice");
            assertThat(claims.get("typ")).isEqualTo("access");
            assertThat(claims.get("tv", Long.class)).isEqualTo(0L);
            assertThat(claims.get("roles")).isEqualTo(List.of("ROLE_USER"));
        }

        @Test
        @DisplayName("refresh token carrega typ=refresh e tv")
        void refreshTokenClaims() {
            final var user = enabledUser("alice");
            final var token = jwtService.generateRefreshToken(user);

            final var claims = Jwts.parserBuilder().setSigningKey(SECRET_KEY).build()
                    .parseClaimsJws(token).getBody();

            assertThat(claims.get("typ")).isEqualTo("refresh");
            assertThat(claims.get("tv", Long.class)).isEqualTo(0L);
            assertThat(claims.getSubject()).isEqualTo("alice");
        }
    }

    @Nested
    @DisplayName("doFilterInternal")
    class Filter {

        @Test
        @DisplayName("token de acesso válido autentica o usuário no SecurityContext")
        void validAccessTokenAuthenticates() throws Exception {
            final var user = enabledUser("alice");
            when(userApiService.loadUserByUsername("alice")).thenReturn(user);
            final var token = rawToken("access", "alice", 60_000, 0L);

            runFilter(token);

            final var auth = SecurityContextHolder.getContext().getAuthentication();
            assertThat(auth).isNotNull();
            assertThat(auth.getName()).isEqualTo("alice");
            assertThat(auth.getCredentials()).isNull();
        }

        @Test
        @DisplayName("token com tokenVersion desatualizada (revogado) não autentica")
        void revokedTokenVersionIsRejected() throws Exception {
            final var user = enabledUser("alice");
            user.setTokenVersion(1L); // logout/troca de senha já incrementou
            when(userApiService.loadUserByUsername("alice")).thenReturn(user);
            final var token = rawToken("access", "alice", 60_000, 0L); // token emitido antes

            runFilter(token);

            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        }

        @Test
        @DisplayName("token malformado não autentica e não propaga exceção")
        void malformedTokenDoesNotAuthenticateOrThrow() throws Exception {
            assertThatNoException(() -> runFilter("token-completamente-invalido"));

            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        }

        @Test
        @DisplayName("refresh token usado como access token não autentica")
        void refreshTokenAsAccessTokenIsRejected() throws Exception {
            final var refreshToken = rawToken("refresh", "alice", 60_000, 0L);

            runFilter(refreshToken);

            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
            org.mockito.Mockito.verifyNoInteractions(userApiService);
        }

        @Test
        @DisplayName("usuário desabilitado com token válido não autentica")
        void disabledAccountIsRejected() throws Exception {
            final var user = enabledUser("alice");
            user.setIsEnabled(false);
            when(userApiService.loadUserByUsername("alice")).thenReturn(user);
            final var token = rawToken("access", "alice", 60_000, 0L);

            runFilter(token);

            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        }

        @Test
        @DisplayName("sem header Authorization, segue a chain sem tocar em userApiService")
        void noAuthHeaderPassesThrough() throws Exception {
            final var request = new MockHttpServletRequest();
            final var response = new MockHttpServletResponse();
            final var chain = mock(FilterChain.class);

            jwtService.doFilter(request, response, chain);

            verify(chain).doFilter(request, response);
            org.mockito.Mockito.verifyNoInteractions(userApiService);
        }

        private void runFilter(String bearerToken) throws Exception {
            final var request = new MockHttpServletRequest();
            request.addHeader("Authorization", "Bearer " + bearerToken);
            final var response = new MockHttpServletResponse();
            final var chain = mock(FilterChain.class);

            jwtService.doFilter(request, response, chain);

            verify(chain).doFilter(request, response);
        }

        private void assertThatNoException(ThrowingRunnable runnable) throws Exception {
            runnable.run();
        }

        private interface ThrowingRunnable {
            void run() throws Exception;
        }
    }

    @Nested
    @DisplayName("validateRefreshToken")
    class ValidateRefreshToken {

        @Test
        @DisplayName("token de refresh válido retorna o usuário")
        void validRefreshTokenReturnsUser() {
            final var user = enabledUser("alice");
            when(userApiService.loadUserByUsername("alice")).thenReturn(user);
            final var token = jwtService.generateRefreshToken(user);

            assertThat(jwtService.validateRefreshToken(token).getUsername()).isEqualTo("alice");
        }

        @Test
        @DisplayName("token expirado lança InvalidRefreshTokenException")
        void expiredTokenThrows() {
            final var expired = rawToken("refresh", "alice", -1_000, 0L);

            assertThatThrownBy(() -> jwtService.validateRefreshToken(expired))
                    .isInstanceOf(InvalidRefreshTokenException.class);
        }

        @Test
        @DisplayName("access token apresentado como refresh é rejeitado")
        void accessTokenRejectedAsRefresh() {
            final var accessToken = jwtService.generateToken(enabledUser("alice"));

            assertThatThrownBy(() -> jwtService.validateRefreshToken(accessToken))
                    .isInstanceOf(InvalidRefreshTokenException.class)
                    .hasMessageContaining("not a refresh token");
        }

        @Test
        @DisplayName("refresh token com tokenVersion desatualizada (revogado) é rejeitado")
        void revokedRefreshTokenIsRejected() {
            final var user = enabledUser("alice");
            final var token = jwtService.generateRefreshToken(user); // tv=0
            user.setTokenVersion(1L); // logout aconteceu depois de emitido
            when(userApiService.loadUserByUsername("alice")).thenReturn(user);

            assertThatThrownBy(() -> jwtService.validateRefreshToken(token))
                    .isInstanceOf(InvalidRefreshTokenException.class)
                    .hasMessageContaining("revoked");
        }

        @Test
        @DisplayName("token com assinatura inválida lança InvalidRefreshTokenException")
        void garbageTokenThrows() {
            assertThatThrownBy(() -> jwtService.validateRefreshToken("nao-e-um-jwt"))
                    .isInstanceOf(InvalidRefreshTokenException.class);
        }
    }
}
