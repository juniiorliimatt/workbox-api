package br.com.workbox;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * @author CLAUDE-CODE
 * @author Junior Lima - oojuniin@outlook.com
 * @since 29-08-2026
 */

@SpringBootApplication
@EnableScheduling
public class WorkBoxApplication {
    public static void main(String[] args) {
        SpringApplication.run(WorkBoxApplication.class, args);
    }
}
