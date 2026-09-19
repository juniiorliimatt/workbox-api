package br.com.workbox.config;

import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;

@Configuration
public class RedisConfig {

    /**
     * Carrega o script Lua que consome um refresh token de forma atômica (GET + validação
     * + rotação num único round-trip ao Redis) — evita race condition entre duas requisições
     * concorrentes tentando trocar o mesmo refresh token, o que também é a base da detecção
     * de reuso (token já consumido sendo apresentado de novo).
     */
    @Bean
    public RedisScript<List> consumeRefreshTokenScript() {
        final var script = new DefaultRedisScript<List>();
        script.setLocation(new ClassPathResource("scripts/consume_refresh_token.lua"));
        script.setResultType(List.class);
        return script;
    }
}
