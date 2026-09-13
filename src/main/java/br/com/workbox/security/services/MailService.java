package br.com.workbox.security.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * @author CLAUDE-CODE
 * @author Junior Lima - oojuniin@outlook.com
 * @since 29-08-2026
 */

@Service
public class MailService {

    private static final Logger logger = LoggerFactory.getLogger(MailService.class);

    private final JavaMailSender mailSender;
    private final String fromAddress;
    private final String frontendBaseUrl;

    public MailService(final JavaMailSender mailSender,
                        @Value("${mail.from:no-reply@workbox.local}") final String fromAddress,
                        @Value("${frontend.base-url:http://localhost:7053}") final String frontendBaseUrl) {
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
        send(message, to);
    }

    /** Disparado só depois que a senha já foi trocada com sucesso — nunca antes. */
    public void sendPasswordResetConfirmationEmail(final String to) {
        final var message = new SimpleMailMessage();
        message.setTo(to);
        message.setFrom(fromAddress);
        message.setSubject("Sua senha foi redefinida");
        message.setText("Sua senha foi alterada com sucesso.\n\n"
                + "Se você não fez essa alteração, troque sua senha novamente agora e "
                + "entre em contato com o suporte.");
        send(message, to);
    }

    private void send(final SimpleMailMessage message, final String to) {
        try {
            mailSender.send(message);
        } catch (MailException e) {
            // Sem SMTP real configurado (padrão em dev/estudo), o envio falha aqui —
            // nunca propagar pro chamador: a operação de negócio (forgot-password,
            // reset-password) já concluiu, o e-mail é só notificação best-effort.
            logger.warn("Failed to send email to {}: {}", to, e.getMessage());
        }
    }
}
