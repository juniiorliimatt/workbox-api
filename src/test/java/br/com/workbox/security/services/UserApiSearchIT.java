package br.com.workbox.security.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import br.com.workbox.security.dto.UserApiDTO;
import br.com.workbox.security.entities.UserApi;
import br.com.workbox.security.repositories.UserApiRepository;
import br.com.workbox.steps.MailTestConfig;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/**
 * {@code UserApiServiceTest} mocka o repository — não exercita a query real do
 * {@code Specification} de busca. Este teste roda contra o H2 real (profile {@code test})
 * pra confirmar que o filtro por {@code search} de fato funciona (socialName OU email,
 * substring, case-insensitive), não só que o repository é chamado.
 */
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Import(MailTestConfig.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@Transactional
class UserApiSearchIT {

    @Autowired
    private UserApiService userApiService;

    @Autowired
    private UserApiRepository userApiRepository;

    @BeforeEach
    void seedUsers() {
        userApiRepository.save(user("Alice Rocha", "alice.rocha@example.com"));
        userApiRepository.save(user("Bob Ferreira", "bob@example.com"));
        userApiRepository.save(user("Carla Alves", "carla.alves@example.com"));
    }

    private UserApi user(String socialName, String email) {
        return UserApi.builder()
                .socialName(socialName)
                .email(email)
                .password("hash")
                .roles(Set.of())
                .build();
    }

    @Test
    @DisplayName("search filtra por socialName, case-insensitive, substring")
    void filtersBySocialName() {
        final var result = userApiService.findAll("rocha", PageRequest.of(0, 10));

        assertThat(result.getContent())
                .extracting("socialName", "email")
                .containsExactly(tuple("Alice Rocha", "alice.rocha@example.com"));
    }

    @Test
    @DisplayName("search filtra por email quando não bate com socialName")
    void filtersByEmail() {
        final var result = userApiService.findAll("bob@example.com", PageRequest.of(0, 10));

        assertThat(result.getContent()).extracting("email").containsExactly("bob@example.com");
    }

    @Test
    @DisplayName("search casa múltiplos usuários pelo mesmo termo (substring em email)")
    void matchesMultipleUsers() {
        final var result = userApiService.findAll("alves", PageRequest.of(0, 10));

        assertThat(result.getContent()).extracting("socialName").containsExactly("Carla Alves");
    }

    @Test
    @DisplayName("search nulo ou vazio não filtra nada")
    void nullOrBlankSearchReturnsAll() {
        // Contexto de Spring/H2 é compartilhado entre classes de teste na mesma execução —
        // não afirma total exato (pode ter sobra de outras suítes), só que os 3 usuários
        // desta classe aparecem quando não há filtro.
        final var expected = List.of("alice.rocha@example.com", "bob@example.com", "carla.alves@example.com");

        assertThat(emailsOf(userApiService.findAll(null, PageRequest.of(0, 1000)))).containsAll(expected);
        assertThat(emailsOf(userApiService.findAll("  ", PageRequest.of(0, 1000)))).containsAll(expected);
    }

    private List<String> emailsOf(Page<UserApiDTO> page) {
        return page.getContent().stream().map(UserApiDTO::getEmail).toList();
    }

    @Test
    @DisplayName("search sem correspondência retorna página vazia")
    void noMatchReturnsEmptyPage() {
        final var result = userApiService.findAll("ninguem-com-esse-nome", PageRequest.of(0, 10));

        assertThat(result.getTotalElements()).isZero();
    }
}
