package br.com.workbox.steps;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

import br.com.workbox.security.entities.Role;
import br.com.workbox.security.entities.UserApi;
import br.com.workbox.security.repositories.UserApiRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.cucumber.java.pt.Dado;
import io.cucumber.java.pt.Então;
import io.cucumber.java.pt.Quando;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

public class AuthenticationSteps {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserApiRepository userApiRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private HttpResultContext context;

    @Dado("um usuário habilitado {string} com senha {string}")
    public void umUsuarioHabilitadoComSenha(String username, String rawPassword) {
        criarUsuario(username, rawPassword, true);
    }

    @Dado("um usuário desabilitado {string} com senha {string}")
    public void umUsuarioDesabilitadoComSenha(String username, String rawPassword) {
        criarUsuario(username, rawPassword, false);
    }

    @Quando("eu tento autenticar com usuário {string} e senha {string}")
    public void euTentoAutenticarComUsuarioESenha(String username, String password) throws Exception {
        context.setResult(login(username, password));
        capturarAccessTokenSeSucesso();
    }

    @Quando("eu tento autenticar sem sucesso {int} vezes com usuário {string} e senha errada")
    public void euTentoAutenticarSemSucessoVezes(int vezes, String username) throws Exception {
        for (int i = 0; i < vezes; i++) {
            context.setResult(login(username, "senha-definitivamente-errada"));
        }
    }

    @Quando("eu faço logout")
    public void euFacoLogout() throws Exception {
        context.setResult(mockMvc.perform(post("/api/v1/auth/logout")
                        .header("Authorization", "Bearer " + context.getAccessToken()))
                .andReturn());
    }

    @Quando("eu consulto meus dados")
    public void euConsultoMeusDados() throws Exception {
        context.setResult(mockMvc.perform(get("/api/v1/auth/me")
                        .header("Authorization", "Bearer " + context.getAccessToken()))
                .andReturn());
    }

    @Quando("eu tento trocar minha senha de {string} para {string}")
    public void euTentoTrocarMinhaSenha(String senhaAtual, String novaSenha) throws Exception {
        final var body = objectMapper.writeValueAsString(
                new br.com.workbox.security.dto.ChangePasswordDTO(senhaAtual, novaSenha));

        context.setResult(mockMvc.perform(put("/api/v1/auth/password")
                        .header("Authorization", "Bearer " + context.getAccessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andReturn());
    }

    @Então("a resposta é {string}")
    public void aRespostaE(String expectedStatus) {
        final var actual = HttpStatus.valueOf(context.getResult().getResponse().getStatus());
        assertThat(actual).isEqualTo(HttpStatus.valueOf(expectedStatus));
    }

    @Então("um access_token é retornado")
    public void umAccessTokenERetornado() throws Exception {
        final JsonNode json = objectMapper.readTree(context.getResult().getResponse().getContentAsString());
        assertThat(json.get("access_token").asText()).isNotBlank();
    }

    @Então("recebo meu perfil com nome {string}")
    public void reboMeuPerfilComNome(String name) throws Exception {
        assertThat(context.getResult().getResponse().getStatus()).isEqualTo(HttpStatus.OK.value());
        final JsonNode json = objectMapper.readTree(context.getResult().getResponse().getContentAsString());
        assertThat(json.get("socialName").asText()).isEqualTo(name);
    }

    @Então("minhas requisições autenticadas com o token antigo são rejeitadas")
    public void minhasRequisicoesComTokenAntigoSaoRejeitadas() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer " + context.getAccessToken()))
                .andExpect(MockMvcResultMatchers.status().isUnauthorized());
    }

    private MvcResult login(String username, String password) throws Exception {
        final var body = objectMapper.writeValueAsString(
                new br.com.workbox.security.dto.UserApiLoginCredentialsDTO(username + "@example.com", password));

        return mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andReturn();
    }

    private void capturarAccessTokenSeSucesso() throws Exception {
        if (context.getResult().getResponse().getStatus() != HttpStatus.OK.value()) {
            return;
        }
        final JsonNode json = objectMapper.readTree(context.getResult().getResponse().getContentAsString());
        // Login com MFA habilitado responde 200 sem access_token (mfa_required + mfa_token
        // em vez disso) — nada a capturar até a segunda etapa em MfaSteps.
        if (json.has("access_token")) {
            context.setAccessToken(json.get("access_token").asText());
        }
    }

    private void criarUsuario(String username, String rawPassword, boolean enabled) {
        final var role = Role.builder().authority("USER").build();
        final var user = UserApi.builder()
                .socialName(username)
                .email(username + "@example.com")
                .password(passwordEncoder.encode(rawPassword))
                .isEnabled(enabled)
                .isAccountNonExpired(true)
                .isAccountNonLocked(true)
                .isCredentialsNonExpired(true)
                .roles(Set.of(role))
                .build();
        userApiRepository.save(user);
    }
}
