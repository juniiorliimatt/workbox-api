package br.com.workbox.security.config;

import br.com.workbox.security.services.JwtService;
import br.com.workbox.security.services.UserApiService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private static final String ROLE_ADMIN = "ADMIN";
    private static final String ROLE_USER = "USER";
    public static final String BAR = "/";
    public static final String USER = "user";

    private final Environment env;
    private final JwtService jwtService;
    private final JwtAuthenticationConverter jwtAuthenticationConverter;
    private final UserApiService userApiService;

    @Autowired
    public SecurityConfig(Environment env, JwtService jwtService, JwtAuthenticationConverter jwtAuthenticationConverter, UserApiService userApiService) {
        this.env = env;
        this.jwtService = jwtService;
        this.jwtAuthenticationConverter = jwtAuthenticationConverter;
        this.userApiService = userApiService;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity httpSecurity) throws Exception {
        configureHeadersForTestProfile(httpSecurity);
        httpSecurity.csrf(AbstractHttpConfigurer::disable);
        httpSecurity.httpBasic(AbstractHttpConfigurer::disable);
        httpSecurity.cors(cors -> cors.configurationSource(corsConfigurationSource()));

        httpSecurity.authorizeHttpRequests(auth -> auth
                .requestMatchers("/h2-console/**").permitAll()
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers("/swagger-ui/**", "/v3/api-docs", "/v3/api-docs/**", "/v3/api-docs.yaml").permitAll()
                .requestMatchers(HttpMethod.GET, BAR + USER).hasAnyRole(ROLE_ADMIN, ROLE_USER)
                .requestMatchers(HttpMethod.POST, BAR + USER).hasRole(ROLE_ADMIN)
                .requestMatchers(HttpMethod.PUT, BAR + USER).hasRole(ROLE_ADMIN)
                .requestMatchers(HttpMethod.DELETE, BAR + USER + "/**").hasRole(ROLE_ADMIN)
                .requestMatchers("/","/*.html", "/assets/*.css", "/assets/*.js", "/assets/*.svg", "/assets/*.png", "/*.png").permitAll()
                .anyRequest().authenticated()
        );

        httpSecurity.oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter)));

        httpSecurity.userDetailsService(userApiService).exceptionHandling(exceptionHandling ->
                exceptionHandling.authenticationEntryPoint(
                        (request, response, authException) ->
                                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized")
                )
        );

        httpSecurity.addFilterBefore(jwtService, UsernamePasswordAuthenticationFilter.class);

        return httpSecurity.build();
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
