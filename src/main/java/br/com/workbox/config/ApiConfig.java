package br.com.workbox.config;

import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

/**
 * @author CLAUDE-CODE
 * @author Junior Lima - oojuniin@outlook.com
 * @since 29-08-2026
 */

@Configuration
@EnableJpaAuditing
public class ApiConfig implements WebMvcConfigurer {

    @Value("${admin.password}")
    private String adminPassword;

    @Value("${jwt.secret}")
    private String secret;

    /** Chave HS256 usada por {@code JwtService} pra assinar/validar os JWTs — deriva de {@code jwt.secret}. */
    @Bean
    public SecretKey secretKey() {
        return new SecretKeySpec(secret.getBytes(), SignatureAlgorithm.HS256.getJcaName());
    }

    /** BCrypt com custo 12 — usado pra hash de senha de usuário e de client_secret dos resource servers. */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }
}
