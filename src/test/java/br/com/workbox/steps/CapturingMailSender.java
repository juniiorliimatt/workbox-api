package br.com.workbox.steps;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSenderImpl;

/**
 * Substitui o JavaMailSender real nos testes — nunca abre conexão SMTP, só guarda a
 * última mensagem "enviada" pra os steps de Cucumber inspecionarem (ex.: extrair o
 * token de reset de senha do corpo do e-mail).
 */
public class CapturingMailSender extends JavaMailSenderImpl {

    private SimpleMailMessage lastMessage;

    @Override
    public void send(SimpleMailMessage simpleMessage) {
        this.lastMessage = simpleMessage;
    }

    public SimpleMailMessage getLastMessage() {
        return lastMessage;
    }
}
