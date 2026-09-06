package br.com.workbox.security.config;

import static org.hamcrest.Matchers.is;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.workbox.security.entities.ApiClient;
import br.com.workbox.security.entities.ApiClientGrantType;
import br.com.workbox.security.entities.UserApi;
import br.com.workbox.security.repositories.ApiClientRepository;
import br.com.workbox.security.repositories.UserApiRepository;
import br.com.workbox.security.services.JwtService;
import br.com.workbox.steps.MailTestConfig;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cobre a coexistência das duas {@code SecurityFilterChain} (ver {@link SecurityConfig}):
 * {@code /api/v1/auth/introspect} só aceita client credentials (HTTP Basic), nunca o
 * {@code JwtService} de usuário — e o resto da API continua exigindo Bearer normalmente.
 * Unit tests de {@code JwtServiceTest}/{@code AuthControllerTest} já cobrem a lógica de
 * introspecção isolada; o que só um teste de integração real prova é que o
 * {@code @Order} + {@code securityMatcher} das duas chains não colidem.
 */
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Import(MailTestConfig.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@Transactional
class IntrospectionSecurityIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserApiRepository userApiRepository;

    @Autowired
    private ApiClientRepository apiClientRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    private static final String CLIENT_ID = "budget-service";
    private static final String CLIENT_SECRET = "test-secret";

    @BeforeEach
    void seedApiClient() {
        apiClientRepository.save(ApiClient.builder()
                .name("budget-service")
                .clientId(CLIENT_ID)
                .clientSecretHash(passwordEncoder.encode(CLIENT_SECRET))
                .allowedGrantTypes(Set.of(ApiClientGrantType.CLIENT_SECRET))
                .active(true)
                .build());
    }

    @Test
    @DisplayName("sem credenciais responde 401")
    void noCredentials() throws Exception {
        mockMvc.perform(post("/api/v1/auth/introspect")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("token", "whatever"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("client credentials erradas respondem 401")
    void wrongCredentials() throws Exception {
        mockMvc.perform(post("/api/v1/auth/introspect")
                        .with(httpBasic(CLIENT_ID, "senha-errada"))
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("token", "whatever"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("bearer de usuário não autentica nessa rota — só client credentials")
    void userBearerTokenIsRejected() throws Exception {
        final var user = registerUser("bearer-nao-vale@example.com");
        final var userAccessToken = jwtService.generateToken(user);

        mockMvc.perform(post("/api/v1/auth/introspect")
                        .header("Authorization", "Bearer " + userAccessToken)
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("token", userAccessToken))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("client credentials corretas + access token válido retorna active=true")
    void validTokenIsActive() throws Exception {
        final var user = registerUser("introspeccao-valida@example.com");
        final var accessToken = jwtService.generateToken(user);

        mockMvc.perform(post("/api/v1/auth/introspect")
                        .with(httpBasic(CLIENT_ID, CLIENT_SECRET))
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("token", accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active", is(true)))
                .andExpect(jsonPath("$.sub", is("introspeccao-valida@example.com")));
    }

    @Test
    @DisplayName("client credentials corretas + token inválido retorna active=false, não 401")
    void invalidTokenIsInactiveNotUnauthorized() throws Exception {
        mockMvc.perform(post("/api/v1/auth/introspect")
                        .with(httpBasic(CLIENT_ID, CLIENT_SECRET))
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("token", "token-completamente-invalido"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active", is(false)));
    }

    @Test
    @DisplayName("client credentials corretas + token vazio retorna active=false, não 500")
    void blankTokenIsInactiveNotServerError() throws Exception {
        mockMvc.perform(post("/api/v1/auth/introspect")
                        .with(httpBasic(CLIENT_ID, CLIENT_SECRET))
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("token", ""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active", is(false)));
    }

    /**
     * Persiste direto pelo repository (não {@code UserApiService.register}) pra não
     * depender de {@code RoleRepository.findByAuthority("USER")} — a introspecção não
     * precisa de role nenhuma persistida, só de um usuário utilizável.
     */
    private UserApi registerUser(String email) {
        final var user = UserApi.builder()
                .socialName("Introspect Test")
                .email(email)
                .password("hash")
                .roles(Set.of())
                .build();
        return userApiRepository.save(user);
    }
}
