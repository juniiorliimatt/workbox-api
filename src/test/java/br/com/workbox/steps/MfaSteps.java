package br.com.workbox.steps;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import br.com.workbox.security.dto.MfaCodeDTO;
import br.com.workbox.security.dto.MfaLoginDTO;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.samstevens.totp.code.DefaultCodeGenerator;
import io.cucumber.java.pt.Então;
import io.cucumber.java.pt.Quando;
import java.time.Instant;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

public class MfaSteps {

    private static final int TIME_PERIOD_SECONDS = 30;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private HttpResultContext context;

    private final DefaultCodeGenerator codeGenerator = new DefaultCodeGenerator();
    private String secret;
    private String mfaToken;

    @Quando("eu habilito o MFA")
    public void euHabilitoOMfa() throws Exception {
        final var result = mockMvc.perform(post("/api/v1/auth/mfa/enroll")
                        .header("Authorization", "Bearer " + context.getAccessToken()))
                .andReturn();
        final JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        this.secret = json.get("secret").asText();
    }

    @Quando("eu confirmo o MFA com o código correto")
    public void euConfirmoOMfaComOCodigoCorreto() throws Exception {
        final var body = objectMapper.writeValueAsString(new MfaCodeDTO(currentCode()));
        context.setResult(mockMvc.perform(post("/api/v1/auth/mfa/verify")
                        .header("Authorization", "Bearer " + context.getAccessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andReturn());
    }

    @Quando("eu desabilito o MFA com o código correto")
    public void euDesabilitoOMfaComOCodigoCorreto() throws Exception {
        final var body = objectMapper.writeValueAsString(new MfaCodeDTO(currentCode()));
        context.setResult(mockMvc.perform(post("/api/v1/auth/mfa/disable")
                        .header("Authorization", "Bearer " + context.getAccessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andReturn());
    }

    @Então("o login exige um segundo fator")
    public void oLoginExigeUmSegundoFator() throws Exception {
        final JsonNode json = objectMapper.readTree(context.getResult().getResponse().getContentAsString());
        assertThat(json.get("mfa_required").asBoolean()).isTrue();
        this.mfaToken = json.get("mfa_token").asText();
    }

    @Quando("eu envio o código correto de MFA")
    public void euEnvioOCodigoCorretoDeMfa() throws Exception {
        enviarCodigoDeMfa(currentCode());
    }

    @Quando("eu envio o código {string} de MFA")
    public void euEnvioOCodigoDeMfa(String code) throws Exception {
        enviarCodigoDeMfa(code);
    }

    private void enviarCodigoDeMfa(String code) throws Exception {
        final var body = objectMapper.writeValueAsString(new MfaLoginDTO(mfaToken, code));
        context.setResult(mockMvc.perform(post("/api/v1/auth/mfa/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andReturn());
        if (context.getResult().getResponse().getStatus() == 200) {
            final JsonNode json = objectMapper.readTree(context.getResult().getResponse().getContentAsString());
            if (json.has("access_token")) {
                context.setAccessToken(json.get("access_token").asText());
            }
        }
    }

    private String currentCode() throws Exception {
        final long counter = Instant.now().getEpochSecond() / TIME_PERIOD_SECONDS;
        return codeGenerator.generate(secret, counter);
    }
}
