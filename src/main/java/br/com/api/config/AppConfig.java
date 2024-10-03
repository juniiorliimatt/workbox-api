package br.com.api.config;

import br.com.api.security.entities.UserApi;
import br.com.api.security.repositories.UserApiRepository;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.time.LocalDateTime;

@Configuration
@EnableJpaAuditing
public class AppConfig {

    private static final String ADMIN = "admin";

    @Value("${admin.password}")
    private String adminPassword;

    @Value("${jwt.secret}")
    private String secret;

    @Bean
    public SecretKey secretKey() {
        return new SecretKeySpec(secret.getBytes(), SignatureAlgorithm.HS256.getJcaName());
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CommandLineRunner loadData(UserApiRepository userApiRepository, PasswordEncoder passwordEncoder) {
        return args -> userApiRepository.save(
                new UserApi(null,
                        ADMIN,
                        passwordEncoder.encode(adminPassword),
                        true,
                        LocalDateTime.now(),
                        LocalDateTime.now(),
                        ADMIN,
                        ADMIN));
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        return new JwtAuthenticationConverter();
    }
}
