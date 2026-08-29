package br.com.workbox.steps;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

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

public class AuthenticationSteps {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserApiRepository userApiRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ObjectMapper objectMapper;

    private MvcResult result;

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
        final var body = objectMapper.writeValueAsString(
                new br.com.workbox.security.dto.UserApiLoginCredentialsDTO(username, password));

        this.result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andReturn();
    }

    @Então("a resposta é {string}")
    public void aRespostaE(String expectedStatus) {
        final var actual = HttpStatus.valueOf(result.getResponse().getStatus());
        assertThat(actual).isEqualTo(HttpStatus.valueOf(expectedStatus));
    }

    @Então("um access_token é retornado")
    public void umAccessTokenERetornado() throws Exception {
        final JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        assertThat(json.get("access_token").asText()).isNotBlank();
    }

    private void criarUsuario(String username, String rawPassword, boolean enabled) {
        final var role = Role.builder().authority("USER").build();
        final var user = UserApi.builder()
                .username(username)
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
