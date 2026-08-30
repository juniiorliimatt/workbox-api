package br.com.workbox.steps;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import br.com.workbox.security.repositories.UserApiRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.cucumber.java.pt.Então;
import io.cucumber.java.pt.Quando;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

public class AuditSteps {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserApiRepository userApiRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private HttpResultContext context;

    @Quando("eu consulto o histórico de login do email {string}")
    public void euConsultoOHistoricoDeLoginDoEmail(String email) throws Exception {
        context.setResult(mockMvc.perform(get("/api/v1/audit/logins")
                        .param("email", email)
                        .header("Authorization", "Bearer " + context.getAccessToken()))
                .andReturn());
    }

    @Então("o histórico de login tem pelo menos {int} entradas")
    public void oHistoricoDeLoginTemPeloMenosEntradas(int minimo) throws Exception {
        final JsonNode json = objectMapper.readTree(context.getResult().getResponse().getContentAsString());
        assertThat(json.get("content").size()).isGreaterThanOrEqualTo(minimo);
    }

    @Quando("eu consulto o histórico de alterações do usuário {string}")
    public void euConsultoOHistoricoDeAlteracoesDoUsuario(String username) throws Exception {
        final var user = userApiRepository.findByEmail(username + "@example.com")
                .orElseThrow(() -> new IllegalStateException("Usuário não encontrado pra teste: " + username));
        context.setResult(mockMvc.perform(get("/api/v1/audit/users/" + user.getId() + "/history")
                        .header("Authorization", "Bearer " + context.getAccessToken()))
                .andReturn());
    }

    @Então("o histórico de alterações tem pelo menos {int} revisão do tipo {string}")
    public void oHistoricoDeAlteracoesTemPeloMenosRevisaoDoTipo(int minimo, String revisionType) throws Exception {
        final JsonNode json = objectMapper.readTree(context.getResult().getResponse().getContentAsString());
        long count = 0;
        for (final JsonNode node : json) {
            if (revisionType.equals(node.get("revisionType").asText())) {
                count++;
            }
        }
        assertThat(count).isGreaterThanOrEqualTo(minimo);
    }
}
