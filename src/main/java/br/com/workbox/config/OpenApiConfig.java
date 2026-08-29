package br.com.workbox.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author CLAUDE-CODE
 * @author Junior Lima - oojuniin@outlook.com
 * @since 29-08-2026
 */

@Configuration
public class OpenApiConfig {

    private static final String BEARER_SCHEME_NAME = "bearer-jwt";

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Workbox API")
                        .version("0.0.1")
                        .description("Contrato REST da Workbox API. Este documento é a fonte da verdade "
                                + "para qualquer client (frontend, agentes de IA) que consuma esta API — "
                                + "não assuma comportamento não descrito aqui.")
                        .contact(new Contact().name("Junior Lima").email("oojuniin@outlook.com")))
                .addServersItem(new Server().url("/").description("Relativo ao host onde a API estiver publicada"))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_SCHEME_NAME))
                .components(new Components()
                        .addSecuritySchemes(BEARER_SCHEME_NAME, new SecurityScheme()
                                .name(BEARER_SCHEME_NAME)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")));
    }
}
