package br.com.workbox.security.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class MailService {

    private static final Logger logger = LoggerFactory.getLogger(MailService.class);

    private final JavaMailSender mailSender;
    private final String fromAddress;
    private final String frontendBaseUrl;

    public MailService(final JavaMailSender mailSender,
                        @Value("${mail.from:no-reply@workbox.local}") final String fromAddress,
                        @Value("${frontend.base-url:http://localhost:5173}") final String frontendBaseUrl) {
        this.mailSender = mailSender;
        this.fromAddress = fromAddress;
        this.frontendBaseUrl = frontendBaseUrl;
    }

    public void sendPasswordResetEmail(final String to, final String rawToken) {
        final var link = frontendBaseUrl + "/reset-password?token=" + rawToken;
        final var message = new SimpleMailMessage();
        message.setTo(to);
        message.setFrom(fromAddress);
        message.setSubject("Redefinição de senha");
        message.setText("Clique no link abaixo pra redefinir sua senha. Expira em 30 minutos.\n\n" + link
                + "\n\nSe você não pediu isso, ignore este e-mail.");
        try {
            mailSender.send(message);
        } catch (MailException e) {
            // Sem SMTP real configurado (padrão em dev/estudo), o envio falha aqui —
            // nunca propagar pro chamador: forgot-password sempre responde de forma
            // idêntica exista ou não o e-mail, envio tendo funcionado ou não.
            logger.warn("Failed to send password reset email to {}: {}", to, e.getMessage());
        }
    }
}
