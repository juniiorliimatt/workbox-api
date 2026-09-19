package br.com.workbox.security.controllers;

import java.util.Locale;
import java.util.Optional;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.support.MessageSourceAccessor;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.data.domain.AuditorAware;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.web.SecurityFilterChain;

@TestConfiguration
public class ApiControllerTestConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(final HttpSecurity httpSecurity) throws Exception {
        return httpSecurity
                .headers(headers -> headers.frameOptions(HeadersConfigurer.FrameOptionsConfig::disable))
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                .build();
    }

    @Bean
    public AuditorAware<String> auditorAware() {
        return () -> Optional.of("test-user");
    }

    /**
     * {@code @WebMvcTest} instancia {@code RestExceptionHandler}/{@code AuthController}/
     * {@code SecurityConfig} (todo {@code @ControllerAdvice}/bean do slice web), que agora
     * dependem de {@link MessageSourceAccessor} — sem esse bean aqui, todo teste que
     * importa esta config quebra na subida do contexto.
     */
    @Bean
    public MessageSourceAccessor messageSourceAccessor() {
        final var messageSource = new ResourceBundleMessageSource();
        messageSource.setBasename("messages");
        messageSource.setDefaultEncoding("UTF-8");
        return new MessageSourceAccessor(messageSource, Locale.of("pt", "BR"));
    }
}
