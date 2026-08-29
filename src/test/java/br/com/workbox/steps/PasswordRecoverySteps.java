package br.com.workbox.steps;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import br.com.workbox.security.dto.ForgotPasswordDTO;
import br.com.workbox.security.dto.ResetPasswordDTO;
import br.com.workbox.security.dto.UserApiLoginCredentialsDTO;
import br.com.workbox.security.entities.Role;
import br.com.workbox.security.entities.UserApi;
import br.com.workbox.security.repositories.UserApiRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.cucumber.java.pt.Dado;
import io.cucumber.java.pt.Então;
import io.cucumber.java.pt.Quando;
import java.util.Set;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

public class PasswordRecoverySteps {

    private static final Pattern TOKEN_PATTERN = Pattern.compile("token=([^\\s&]+)");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserApiRepository userApiRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CapturingMailSender capturingMailSender;

    @Autowired
    private HttpResultContext context;

    private String lastCapturedToken;

    @Dado("um usuário habilitado {string} com senha {string} e e-mail {string}")
    public void umUsuarioHabilitadoComSenhaEEmail(String username, String rawPassword, String email) {
        final var role = Role.builder().authority("USER").build();
        final var user = UserApi.builder()
                .username(username)
                .password(passwordEncoder.encode(rawPassword))
                .email(email)
                .isEnabled(true)
                .isAccountNonExpired(true)
                .isAccountNonLocked(true)
                .isCredentialsNonExpired(true)
                .roles(Set.of(role))
                .build();
        userApiRepository.save(user);
    }

    @Quando("eu peço recuperação de senha para o e-mail {string}")
    public void euPecoRecuperacaoDeSenha(String email) throws Exception {
        final var body = objectMapper.writeValueAsString(new ForgotPasswordDTO(email));
        context.setResult(mockMvc.perform(post("/api/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andReturn());
    }

    @Então("um e-mail de redefinição foi enviado para {string}")
    public void umEmailDeRedefinicaoFoiEnviadoPara(String email) {
        final var message = capturingMailSender.getLastMessage();
        assertThat(message).isNotNull();
        assertThat(message.getTo()).contains(email);

        final var matcher = TOKEN_PATTERN.matcher(message.getText());
        assertThat(matcher.find()).as("token no corpo do e-mail").isTrue();
        this.lastCapturedToken = matcher.group(1);
    }

    @Quando("eu redefino a senha com o token recebido para {string}")
    public void euRedefinoASenhaComTokenRecebido(String novaSenha) throws Exception {
        redefinirSenha(lastCapturedToken, novaSenha);
    }

    @Quando("eu redefino a senha com o token {string} para {string}")
    public void euRedefinoASenhaComToken(String token, String novaSenha) throws Exception {
        redefinirSenha(token, novaSenha);
    }

    @Então("eu consigo logar com usuário {string} e senha {string}")
    public void euConsigoLogarComUsuarioESenha(String username, String password) throws Exception {
        final var body = objectMapper.writeValueAsString(new UserApiLoginCredentialsDTO(username, password));
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(MockMvcResultMatchers.status().isOk());
    }

    private void redefinirSenha(String token, String novaSenha) throws Exception {
        final var body = objectMapper.writeValueAsString(new ResetPasswordDTO(token, novaSenha));
        context.setResult(mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andReturn());
    }
}
