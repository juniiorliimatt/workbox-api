package br.com.workbox.steps;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

import br.com.workbox.security.dto.RoleDTO;
import br.com.workbox.security.entities.Role;
import br.com.workbox.security.entities.UserApi;
import br.com.workbox.security.repositories.RoleRepository;
import br.com.workbox.security.repositories.UserApiRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.cucumber.java.pt.Dado;
import io.cucumber.java.pt.Então;
import io.cucumber.java.pt.Quando;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

public class RoleManagementSteps {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserApiRepository userApiRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private HttpResultContext context;

    private final Map<String, Long> roleIdsByAuthority = new HashMap<>();

    @Dado("um usuário habilitado {string} com senha {string} e a role {string}")
    public void umUsuarioHabilitadoComSenhaEARole(String username, String rawPassword, String authority) {
        // Role sempre transiente (id nulo) — anexar uma Role já persistida (detached)
        // aqui dispara PersistentObjectException no cascade PERSIST de UserApi.roles.
        final var role = Role.builder().authority(authority).build();

        final var user = UserApi.builder()
                .username(username)
                .password(passwordEncoder.encode(rawPassword))
                .isEnabled(true)
                .isAccountNonExpired(true)
                .isAccountNonLocked(true)
                .isCredentialsNonExpired(true)
                .roles(Set.of(role))
                .build();
        userApiRepository.save(user);
    }

    @Quando("eu crio a role {string}")
    public void euCrioARole(String authority) throws Exception {
        final var body = objectMapper.writeValueAsString(new RoleDTO(null, authority));
        context.setResult(mockMvc.perform(post("/api/role")
                        .header("Authorization", "Bearer " + context.getAccessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andReturn());

        if (context.getResult().getResponse().getStatus() == 201) {
            final JsonNode json = objectMapper.readTree(context.getResult().getResponse().getContentAsString());
            roleIdsByAuthority.put(authority.toUpperCase(), json.get("id").asLong());
        }
    }

    @Quando("eu atualizo a role {string} para {string}")
    public void euAtualizoARolePara(String authorityAtual, String novaAuthority) throws Exception {
        final var id = roleIdsByAuthority.get(authorityAtual.toUpperCase());
        final var body = objectMapper.writeValueAsString(new RoleDTO(id, novaAuthority));
        context.setResult(mockMvc.perform(put("/api/role/" + id)
                        .header("Authorization", "Bearer " + context.getAccessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andReturn());
        roleIdsByAuthority.put(novaAuthority.toUpperCase(), id);
    }

    @Quando("eu removo a role {string}")
    public void euRemovoARole(String authority) throws Exception {
        final var id = roleIdsByAuthority.get(authority.toUpperCase());
        context.setResult(mockMvc.perform(delete("/api/role/" + id)
                        .header("Authorization", "Bearer " + context.getAccessToken()))
                .andReturn());
    }

    @Quando("eu listo as roles")
    public void euListoAsRoles() throws Exception {
        context.setResult(mockMvc.perform(get("/api/role")
                        .header("Authorization", "Bearer " + context.getAccessToken()))
                .andReturn());
    }

    @Então("a role {string} aparece na listagem")
    public void aRoleApareceNaListagem(String authority) throws Exception {
        final var response = mockMvc.perform(get("/api/role")
                        .header("Authorization", "Bearer " + context.getAccessToken()))
                .andReturn().getResponse().getContentAsString();
        assertThat(response).contains("\"" + authority.toUpperCase() + "\"");
    }

    @Então("a role {string} não aparece mais na listagem")
    public void aRoleNaoApareceMaisNaListagem(String authority) throws Exception {
        final var response = mockMvc.perform(get("/api/role")
                        .header("Authorization", "Bearer " + context.getAccessToken()))
                .andReturn().getResponse().getContentAsString();
        assertThat(response).doesNotContain("\"" + authority.toUpperCase() + "\"");
    }
}
