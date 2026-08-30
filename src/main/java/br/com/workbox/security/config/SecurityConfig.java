package br.com.workbox.security.config;

import br.com.workbox.config.CorrelationIdFilter;
import br.com.workbox.security.oauth2.OAuth2LoginSuccessHandler;
import br.com.workbox.security.services.JwtService;
import br.com.workbox.security.services.UserApiService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.io.IOException;
import java.util.Arrays;

/**
 * @author CLAUDE-CODE
 * @author Junior Lima - oojuniin@outlook.com
 * @since 29-08-2026
 */

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private static final String ROLE_ADMIN = "ADMIN";
    private static final String ROLE_USER = "USER";
    private static final String API_USER = "/api/v1/user/**";
    private static final String API_ROLE = "/api/v1/role/**";

    private final Environment env;
    private final JwtService jwtService;
    private final UserApiService userApiService;
    private final ObjectProvider<ClientRegistrationRepository> clientRegistrationRepositoryProvider;
    private final OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler;
    private final ObjectMapper objectMapper;

    @Autowired
    public SecurityConfig(Environment env, JwtService jwtService,
                           UserApiService userApiService, ObjectProvider<ClientRegistrationRepository> clientRegistrationRepositoryProvider,
                           OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler, ObjectMapper objectMapper) {
        this.env = env;
        this.jwtService = jwtService;
        this.userApiService = userApiService;
        this.clientRegistrationRepositoryProvider = clientRegistrationRepositoryProvider;
        this.oAuth2LoginSuccessHandler = oAuth2LoginSuccessHandler;
        this.objectMapper = objectMapper;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity httpSecurity) throws Exception {
        configureHeadersForTestProfile(httpSecurity);
        httpSecurity.csrf(AbstractHttpConfigurer::disable);
        httpSecurity.httpBasic(AbstractHttpConfigurer::disable);
        httpSecurity.cors(cors -> cors.configurationSource(corsConfigurationSource()));

        httpSecurity.authorizeHttpRequests(auth -> auth
                .requestMatchers("/h2-console/**").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/v1/auth/register", "/api/v1/auth/login", "/api/v1/auth/refresh",
                        "/api/v1/auth/forgot-password", "/api/v1/auth/reset-password", "/api/v1/auth/mfa/login").permitAll()
                .requestMatchers("/oauth2/**", "/login/oauth2/**").permitAll()
                .requestMatchers("/actuator/health", "/actuator/prometheus").permitAll()
                .requestMatchers("/swagger-ui/**", "/v3/api-docs", "/v3/api-docs/**", "/v3/api-docs.yaml").permitAll()
                .requestMatchers(HttpMethod.GET, API_USER).hasAnyRole(ROLE_ADMIN, ROLE_USER)
                .requestMatchers(HttpMethod.POST, API_USER).hasRole(ROLE_ADMIN)
                .requestMatchers(HttpMethod.PUT, API_USER).hasRole(ROLE_ADMIN)
                .requestMatchers(HttpMethod.DELETE, API_USER).hasRole(ROLE_ADMIN)
                .requestMatchers(HttpMethod.GET, API_ROLE).hasAnyRole(ROLE_ADMIN, ROLE_USER)
                .requestMatchers(HttpMethod.POST, API_ROLE).hasRole(ROLE_ADMIN)
                .requestMatchers(HttpMethod.PUT, API_ROLE).hasRole(ROLE_ADMIN)
                .requestMatchers(HttpMethod.DELETE, API_ROLE).hasRole(ROLE_ADMIN)
                .anyRequest().authenticated()
        );

        // Autenticação por Bearer JWT é 100% via JwtService (addFilterBefore abaixo) —
        // sem oauth2ResourceServer().jwt() aqui de propósito. Os dois coexistindo já
        // causou um bug real: o resource server nativo do Spring valida assinatura +
        // expiração e autentica por conta própria, sem saber nada de tokenVersion —
        // logout/troca de senha/reset não invalidavam o access token porque esse
        // segundo caminho autenticava por trás do primeiro mesmo com o token revogado.

        // Login social só ativa se houver ao menos um client registrado (ex.: GOOGLE_CLIENT_ID/
        // GOOGLE_CLIENT_SECRET setados) — sem isso o bean nem existe, e chamar oauth2Login()
        // sem nenhum client registrado quebraria a subida da aplicação.
        if (clientRegistrationRepositoryProvider.getIfAvailable() != null) {
            httpSecurity.oauth2Login(oauth2 -> oauth2.successHandler(oAuth2LoginSuccessHandler));
        }

        // Corpo de erro em ProblemDetail (mesmo shape do RestExceptionHandler) pra 401/403 —
        // sem isso, Spring Security responde via response.sendError()/AccessDeniedException
        // sem passar pelo @RestControllerAdvice, e o client recebia o whitelabel padrão do
        // Spring Boot (HTML) em vez de JSON.
        httpSecurity.userDetailsService(userApiService).exceptionHandling(exceptionHandling -> exceptionHandling
                .authenticationEntryPoint((request, response, authException) ->
                        writeProblemDetail(response, HttpStatus.UNAUTHORIZED, "Unauthorized"))
                .accessDeniedHandler((request, response, accessDeniedException) ->
                        writeProblemDetail(response, HttpStatus.FORBIDDEN, "Forbidden"))
        );

        // CorrelationIdFilter registrado primeiro: Spring Security não permite ancorar um
        // filtro customizado em outro filtro customizado (só em classes padrão como
        // UsernamePasswordAuthenticationFilter) — ambos ancoram no mesmo padrão, e a
        // ordem relativa entre eles fica definida pela ordem de chamada aqui (empate no
        // valor de ordem é resolvido por sort estável, preservando a ordem de inserção).
        // CorrelationIdFilter registrado primeiro: Spring Security não permite ancorar um
        // filtro customizado em outro filtro customizado (só em classes padrão como
        // UsernamePasswordAuthenticationFilter) — ambos ancoram no mesmo padrão, e a
        // ordem relativa entre eles fica definida pela ordem de chamada aqui (empate no
        // valor de ordem é resolvido por sort estável, preservando a ordem de inserção).
        httpSecurity.addFilterBefore(new CorrelationIdFilter(), UsernamePasswordAuthenticationFilter.class);
        httpSecurity.addFilterBefore(jwtService, UsernamePasswordAuthenticationFilter.class);

        return httpSecurity.build();
    }

    private void writeProblemDetail(final HttpServletResponse response, final HttpStatus status, final String detail) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), ProblemDetail.forStatusAndDetail(status, detail));
    }

    private void configureHeadersForTestProfile(HttpSecurity httpSecurity) throws Exception {
        if (Arrays.asList(env.getActiveProfiles()).contains("test")) {
            httpSecurity.headers(headers -> headers.frameOptions(HeadersConfigurer.FrameOptionsConfig::disable));
        }
    }

    @Bean
    public AuthenticationManager authManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration().applyPermitDefaultValues();
        // Origin enviado pelo browser nunca tem barra final — CorsConfiguration faz match exato.
        configuration.setAllowedOrigins(Arrays.asList("http://127.0.0.1:5173","http://localhost:5173"));
        configuration.setAllowedMethods(Arrays.asList("POST", "GET", "PUT", "DELETE", "OPTIONS"));
        final UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

}
