package br.com.workbox.steps;

import io.cucumber.spring.ScenarioScope;
import org.springframework.stereotype.Component;
import org.springframework.test.web.servlet.MvcResult;

/**
 * Estado compartilhado entre classes de step dentro do mesmo cenário — evita duplicar
 * "a resposta é {string}" (e similares) em cada classe, o que o Cucumber rejeita como
 * step ambíguo. Escopo por cenário: uma instância nova a cada cenário, nunca vaza pro
 * próximo. Guarda também o último access_token emitido, pra steps de classes
 * diferentes (ex.: login em AuthenticationSteps, CRUD de role em
 * RoleManagementSteps) reusarem a mesma sessão autenticada.
 */
@Component
@ScenarioScope
public class HttpResultContext {

    private MvcResult result;
    private String accessToken;

    public MvcResult getResult() {
        return result;
    }

    public void setResult(MvcResult result) {
        this.result = result;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }
}
