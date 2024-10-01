package br.com.api;

import br.com.api.security.entities.UserApi;
import br.com.api.security.repositories.UserApiRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.password.PasswordEncoder;

@SpringBootApplication
public class ApiApplication {

    @Value("${admin.password}")
    private String adminPassword;

    public static void main(String[] args) {
        SpringApplication.run(ApiApplication.class, args);
    }

    @Bean
    public CommandLineRunner loadData(UserApiRepository userApiRepository, PasswordEncoder passwordEncoder) {
        return args -> userApiRepository.save(new UserApi(null, "admin", passwordEncoder.encode(adminPassword), true));
    }

}
