package br.com.workbox.config;

import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;

@Configuration
public class RedisConfig {

    @Bean
    public RedisScript<List> consumeRefreshTokenScript() {
        final var script = new DefaultRedisScript<List>();
        script.setLocation(new ClassPathResource("scripts/consume_refresh_token.lua"));
        script.setResultType(List.class);
        return script;
    }
}
