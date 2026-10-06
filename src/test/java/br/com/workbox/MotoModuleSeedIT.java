package br.com.workbox;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/**
 * Garante que as migrations do módulo Moto rodam contra um Postgres real: módulo
 * {@code MOTO} no catálogo, role {@code MOTO} vinculada a ele e o client de introspecção
 * do {@code moto-service}.
 */
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("dev")
class MotoModuleSeedIT {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>(DockerImageName.parse("postgres:18"))
            .withDatabaseName("workbox")
            .withUsername("postgres")
            .withPassword("postgres")
            .withInitScript("testcontainers-init.sql");

    @DynamicPropertySource
    static void datasourceProperties(final DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> "jdbc:postgresql://%s:%d/workbox"
                .formatted(POSTGRES.getHost(), POSTGRES.getMappedPort(5432)));
        registry.add("spring.datasource.username", () -> "workbox_service");
        registry.add("spring.datasource.password", () -> "workbox_service");
    }

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void seedCriaModuloMotoRoleEClient() {
        final var modulos = jdbc.queryForList("SELECT code FROM workbox.modules WHERE code = 'MOTO'", String.class);
        assertThat(modulos).containsExactly("MOTO");

        final var moduloDaRole = jdbc.queryForObject(
                "SELECT m.code FROM workbox.roles r JOIN workbox.modules m ON m.id = r.module_id "
                        + "WHERE r.authority = 'MOTO' AND r.deleted_at IS NULL", String.class);
        assertThat(moduloDaRole).isEqualTo("MOTO");

        final var ativo = jdbc.queryForObject(
                "SELECT active FROM workbox.api_clients WHERE client_id = 'moto-service'", Boolean.class);
        assertThat(ativo).isTrue();
    }
}
