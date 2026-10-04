package br.com.workbox.steps;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

import br.com.workbox.security.entities.AppModule;
import br.com.workbox.security.entities.Role;
import br.com.workbox.security.repositories.ModuleRepository;
import br.com.workbox.security.repositories.RoleRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.cucumber.java.pt.Dado;
import io.cucumber.java.pt.Então;
import io.cucumber.java.pt.Quando;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

public class ModuleAccessSteps {

    private static final long MODULO_INEXISTENTE = 999_999L;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ModuleRepository moduleRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private HttpResultContext context;

    private String meuPerfil;

    @Dado("o módulo {string} chamado {string}")
    public void oModuloChamado(final String code, final String name) {
        // Banco H2 é compartilhado entre cenários — não duplica o código (unique).
        if (moduleRepository.findByCode(code).isEmpty()) {
            moduleRepository.save(AppModule.builder().code(code).name(name).build());
        }
    }

    @Quando("eu vinculo a role {string} ao módulo {string}")
    public void euVinculoARoleAoModulo(final String authority, final String code) throws Exception {
        final var moduleId = moduleRepository.findByCode(code).map(AppModule::getId).orElse(MODULO_INEXISTENTE);
        vincular(authority, "{\"moduleId\":" + moduleId + "}");
    }

    @Quando("eu desvinculo a role {string} de qualquer módulo")
    public void euDesvinculoARoleDeQualquerModulo(final String authority) throws Exception {
        vincular(authority, "{\"moduleId\":null}");
    }

    @Quando("eu listo os módulos")
    public void euListoOsModulos() throws Exception {
        context.setResult(mockMvc.perform(get("/api/v1/module")
                        .header("Authorization", "Bearer " + context.getAccessToken()))
                .andReturn());
    }

    @Quando("eu consulto o meu perfil")
    public void euConsultoMeusDados() throws Exception {
        meuPerfil = mockMvc.perform(get("/api/v1/auth/me")
                        .header("Authorization", "Bearer " + context.getAccessToken()))
                .andReturn().getResponse().getContentAsString();
    }

    @Então("o módulo {string} aparece na listagem de módulos")
    public void oModuloApareceNaListagemDeModulos(final String code) throws Exception {
        final var json = objectMapper.readTree(context.getResult().getResponse().getContentAsString());
        final var codes = new ArrayList<String>();
        json.forEach(node -> codes.add(node.get("code").asText()));
        assertThat(codes).contains(code);
    }

    @Então("os módulos do usuário são {string}")
    public void osModulosDoUsuarioSao(final String code) throws Exception {
        assertThat(modulosDoPerfil()).containsExactly(code);
    }

    @Então("os módulos do usuário incluem {string} e {string}")
    public void osModulosDoUsuarioIncluem(final String primeiro, final String segundo) throws Exception {
        assertThat(modulosDoPerfil()).contains(primeiro, segundo);
    }

    @Então("o usuário não tem módulos")
    public void oUsuarioNaoTemModulos() throws Exception {
        assertThat(modulosDoPerfil()).isEmpty();
    }

    private void vincular(final String authority, final String body) throws Exception {
        final var roleId = roleRepository.findAll().stream()
                .filter(role -> authority.equalsIgnoreCase(role.getAuthority()))
                .findFirst()
                .map(Role::getId)
                .orElseThrow(() -> new IllegalStateException("Role não encontrada pra teste: " + authority));
        context.setResult(mockMvc.perform(put("/api/v1/role/" + roleId + "/module")
                        .header("Authorization", "Bearer " + context.getAccessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andReturn());
    }

    private List<String> modulosDoPerfil() throws Exception {
        final JsonNode json = objectMapper.readTree(meuPerfil);
        final var codes = new ArrayList<String>();
        json.get("modules").forEach(node -> codes.add(node.asText()));
        return codes;
    }
}
