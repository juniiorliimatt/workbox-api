package br.com.workbox.steps;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

@TestConfiguration
public class MailTestConfig {

    @Bean
    @Primary
    public CapturingMailSender capturingMailSender() {
        return new CapturingMailSender();
    }
}
