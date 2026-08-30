package br.com.workbox.security.oauth2;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.workbox.security.entities.Role;
import br.com.workbox.security.entities.UserApi;
import br.com.workbox.security.repositories.RoleRepository;
import br.com.workbox.security.repositories.UserApiRepository;
import br.com.workbox.security.services.JwtService;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.user.OAuth2User;

class OAuth2LoginSuccessHandlerTest {

    private UserApiRepository userApiRepository;
    private RoleRepository roleRepository;
    private JwtService jwtService;
    private PasswordEncoder passwordEncoder;
    private OAuth2LoginSuccessHandler handler;

    @BeforeEach
    void setUp() {
        userApiRepository = mock(UserApiRepository.class);
        roleRepository = mock(RoleRepository.class);
        jwtService = mock(JwtService.class);
        passwordEncoder = mock(PasswordEncoder.class);
        handler = new OAuth2LoginSuccessHandler(userApiRepository, roleRepository, jwtService, passwordEncoder, "http://localhost:5173");
    }

    private Authentication authenticationWith(final String email, final String name) {
        final var oAuth2User = mock(OAuth2User.class);
        when(oAuth2User.getAttribute("email")).thenReturn(email);
        when(oAuth2User.getAttribute("name")).thenReturn(name);
        final var authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(oAuth2User);
        return authentication;
    }

    @Test
    @DisplayName("usuário já existente não é reprovisionado — só emite tokens e redireciona")
    void existingUserIsNotProvisioned() throws Exception {
        final var user = UserApi.builder().socialName("Alice").email("alice@example.com").password("hash").build();
        when(userApiRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(user));
        when(jwtService.generateToken(user)).thenReturn("access");
        when(jwtService.issueRefreshToken(user)).thenReturn("refresh");
        final var request = new MockHttpServletRequest();
        final var response = new MockHttpServletResponse();

        handler.onAuthenticationSuccess(request, response, authenticationWith("alice@example.com", "Alice"));

        assertThat(response.getRedirectedUrl())
                .startsWith("http://localhost:5173/oauth2/callback")
                .contains("access_token=access")
                .contains("refresh_token=refresh");
        verify(roleRepository, never()).findAll();
    }

    @Nested
    @DisplayName("provisionUser (usuário novo)")
    class Provisioning {

        @Test
        @DisplayName("usa o atributo 'name' do provider social como socialName")
        void usesProviderNameAsSocialName() throws Exception {
            final var userRole = Role.builder().id(2L).authority("USER").build();
            when(userApiRepository.findByEmail("nova@example.com")).thenReturn(Optional.empty());
            when(roleRepository.findAll()).thenReturn(List.of(userRole));
            when(passwordEncoder.encode(anyString())).thenReturn("hash");
            when(userApiRepository.save(any(UserApi.class))).thenAnswer(inv -> inv.getArgument(0));
            when(jwtService.generateToken(any())).thenReturn("access");
            when(jwtService.issueRefreshToken(any())).thenReturn("refresh");
            final var response = new MockHttpServletResponse();

            handler.onAuthenticationSuccess(new MockHttpServletRequest(), response, authenticationWith("nova@example.com", "Nova Usuária"));

            final var captor = ArgumentCaptor.forClass(UserApi.class);
            verify(userApiRepository).save(captor.capture());
            assertThat(captor.getValue().getSocialName()).isEqualTo("Nova Usuária");
            assertThat(captor.getValue().getEmail()).isEqualTo("nova@example.com");
            assertThat(captor.getValue().getRoles()).containsExactly(userRole);
        }

        @Test
        @DisplayName("usa o email como socialName quando o provider não manda o atributo 'name'")
        void fallsBackToEmailWhenNameIsBlank() throws Exception {
            final var userRole = Role.builder().id(2L).authority("USER").build();
            when(userApiRepository.findByEmail("semnome@example.com")).thenReturn(Optional.empty());
            when(roleRepository.findAll()).thenReturn(List.of(userRole));
            when(passwordEncoder.encode(anyString())).thenReturn("hash");
            when(userApiRepository.save(any(UserApi.class))).thenAnswer(inv -> inv.getArgument(0));
            when(jwtService.generateToken(any())).thenReturn("access");
            when(jwtService.issueRefreshToken(any())).thenReturn("refresh");

            handler.onAuthenticationSuccess(new MockHttpServletRequest(), new MockHttpServletResponse(), authenticationWith("semnome@example.com", null));

            final var captor = ArgumentCaptor.forClass(UserApi.class);
            verify(userApiRepository).save(captor.capture());
            assertThat(captor.getValue().getSocialName()).isEqualTo("semnome@example.com");
        }

        @Test
        @DisplayName("lança IllegalStateException se a role USER não existir (seed ausente)")
        void throwsWhenUserRoleMissing() {
            when(userApiRepository.findByEmail("nova@example.com")).thenReturn(Optional.empty());
            when(roleRepository.findAll()).thenReturn(List.of());

            org.assertj.core.api.Assertions.assertThatThrownBy(() ->
                            handler.onAuthenticationSuccess(new MockHttpServletRequest(), new MockHttpServletResponse(), authenticationWith("nova@example.com", "Nova")))
                    .isInstanceOf(IllegalStateException.class);
        }
    }
}
