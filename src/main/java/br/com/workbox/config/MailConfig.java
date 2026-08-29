package br.com.workbox.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.util.Properties;

/**
 * Bean de {@link JavaMailSender} definido manualmente (não via
 * spring-boot-autoconfigure) para sempre existir mesmo sem SMTP real
 * configurado — a conexão só é tentada no envio, nunca na subida da
 * aplicação. Sem credenciais reais, o envio falha e é logado (ver
 * MailService), não derruba a request.
 */
@Configuration
public class MailConfig {

    @Value("${mail.host:localhost}")
    private String host;

    @Value("${mail.port:25}")
    private int port;

    @Value("${mail.username:}")
    private String username;

    @Value("${mail.password:}")
    private String password;

    @Bean
    public JavaMailSender javaMailSender() {
        final var sender = new JavaMailSenderImpl();
        sender.setHost(host);
        sender.setPort(port);
        sender.setUsername(username);
        sender.setPassword(password);

        final Properties props = sender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        return sender;
    }
}
