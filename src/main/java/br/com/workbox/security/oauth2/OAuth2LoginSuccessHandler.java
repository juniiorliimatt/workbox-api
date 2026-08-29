package br.com.workbox.security.oauth2;

import br.com.workbox.security.entities.Role;
import br.com.workbox.security.entities.UserApi;
import br.com.workbox.security.repositories.RoleRepository;
import br.com.workbox.security.repositories.UserApiRepository;
import br.com.workbox.security.services.JwtService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.net.URLEncoder;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HashSet;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

/**
 * Login social (OAuth2) emite os mesmos access/refresh tokens do login local — o resto
 * da API continua stateless via JWT, o OAuth2 é só mais uma forma de chegar até eles.
 * Provisiona o usuário automaticamente no primeiro login (role USER, senha aleatória
 * inutilizável — a conta só autentica via provider social).
 *
 * @author CLAUDE-CODE
 * @author Junior Lima - oojuniin@outlook.com
 * @since 29-08-2026
 */
@Component
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

    private static final String DEFAULT_ROLE = "USER";

    private final UserApiRepository userApiRepository;
    private final RoleRepository roleRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final String frontendBaseUrl;
    private final SecureRandom secureRandom = new SecureRandom();

    public OAuth2LoginSuccessHandler(final UserApiRepository userApiRepository,
                                      final RoleRepository roleRepository,
                                      final JwtService jwtService,
                                      final PasswordEncoder passwordEncoder,
                                      @Value("${frontend.base-url:http://localhost:5173}") final String frontendBaseUrl) {
        this.userApiRepository = userApiRepository;
        this.roleRepository = roleRepository;
        this.jwtService = jwtService;
        this.passwordEncoder = passwordEncoder;
        this.frontendBaseUrl = frontendBaseUrl;
    }

    @Override
    public void onAuthenticationSuccess(final HttpServletRequest request, final HttpServletResponse response,
                                         final Authentication authentication) throws IOException {
        final var oAuth2User = (OAuth2User) authentication.getPrincipal();
        final String email = oAuth2User.getAttribute("email");

        final var user = userApiRepository.findByEmail(email).orElseGet(() -> provisionUser(email));

        final var accessToken = jwtService.generateToken(user);
        final var refreshToken = jwtService.issueRefreshToken(user);

        final var redirectUrl = frontendBaseUrl + "/oauth2/callback"
                + "?access_token=" + URLEncoder.encode(accessToken, StandardCharsets.UTF_8)
                + "&refresh_token=" + URLEncoder.encode(refreshToken, StandardCharsets.UTF_8);
        response.sendRedirect(redirectUrl);
    }

    private UserApi provisionUser(final String email) {
        final var defaultRole = roleRepository.findAll().stream()
                .filter(role -> DEFAULT_ROLE.equals(role.getAuthority()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Default role USER not found — seed data missing"));

        final var user = UserApi.builder()
                .username(email)
                .email(email)
                .password(passwordEncoder.encode(randomUnusablePassword()))
                .isEnabled(true)
                .roles(new HashSet<>(java.util.Set.of(defaultRole)))
                .build();
        return userApiRepository.save(user);
    }

    private String randomUnusablePassword() {
        final var bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getEncoder().encodeToString(bytes);
    }
}
