package br.com.workbox.steps;

import java.util.ArrayList;
import java.util.List;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSenderImpl;

/**
 * Substitui o JavaMailSender real nos testes — nunca abre conexão SMTP, só guarda as
 * mensagens "enviadas" pra os steps de Cucumber inspecionarem (ex.: extrair o token de
 * reset de senha do corpo do e-mail, ou conferir que o e-mail de confirmação saiu depois
 * do de redefinição). Bean é singleton no contexto do Cucumber/Spring, compartilhado
 * entre cenários — {@link #clear()} precisa rodar num {@code @Before} de cada cenário
 * que usa isso, senão mensagem de um cenário vaza pro próximo.
 */
public class CapturingMailSender extends JavaMailSenderImpl {

    private final List<SimpleMailMessage> messages = new ArrayList<>();

    @Override
    public void send(final SimpleMailMessage simpleMessage) {
        messages.add(simpleMessage);
    }

    public SimpleMailMessage getLastMessage() {
        return messages.isEmpty() ? null : messages.get(messages.size() - 1);
    }

    public List<SimpleMailMessage> getMessages() {
        return messages;
    }

    public void clear() {
        messages.clear();
    }
}
