package br.com.workbox;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/**
 * Sobe o contexto Spring inteiro (Liquibase rodando todas as migrations + Hibernate
 * {@code ddl-auto=validate}) contra um Postgres real, com o mesmo role/schema restrito
 * de produção ({@code workbox_service}/{@code workbox}, ver
 * {@code testcontainers-init.sql}) — não o superusuário do container.
 *
 * Automatiza o que esta sessão fez manualmente 3 vezes (subir um Postgres descartável +
 * {@code bootRun --args='--spring.profiles.active=dev'}) pra pegar drift de schema entre
 * entidade JPA e changelog Liquibase — os 3 erros de mapeamento do Envers descobertos
 * nesta sessão só apareceram contra Postgres real, nunca no H2 ({@code create-drop}) dos
 * outros testes.
 */
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("dev")
class RealPostgresSchemaIT {

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

    @Test
    void applicationContextLoadsAgainstRealPostgresSchema() {
        // O teste é o próprio carregamento do contexto: se Liquibase e o mapeamento JPA
        // divergirem, ddl-auto=validate falha o boot antes de chegar aqui.
    }
}
