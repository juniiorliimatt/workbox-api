package br.com.workbox.security.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.workbox.security.entities.AppModule;
import br.com.workbox.security.entities.Role;
import br.com.workbox.security.entities.UserApi;
import br.com.workbox.security.repositories.ModuleRepository;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ModuleAccessServiceTest {

    private ModuleRepository moduleRepository;
    private ModuleAccessService service;

    @BeforeEach
    void setUp() {
        moduleRepository = mock(ModuleRepository.class);
        service = new ModuleAccessService(moduleRepository);
    }

    private static AppModule module(final long id, final String code) {
        return AppModule.builder().id(id).code(code).name(code).build();
    }

    private static UserApi userWith(final Role... roles) {
        return UserApi.builder().roles(Set.of(roles)).build();
    }

    @Test
    @DisplayName("ADMIN recebe todos os módulos do catálogo")
    void adminGetsEveryModule() {
        when(moduleRepository.findAll()).thenReturn(List.of(module(1, "FINANCAS"), module(2, "FORZA")));

        final var codes = service.codigosDo(userWith(Role.builder().id(1L).authority("ADMIN").build()));

        assertThat(codes).containsExactlyInAnyOrder("FINANCAS", "FORZA");
    }

    @Test
    @DisplayName("usuário comum recebe só os módulos das suas roles")
    void regularUserGetsOnlyLinkedModules() {
        final var financeiro = Role.builder().id(3L).authority("FINANCAS").module(module(1, "FINANCAS")).build();
        final var user = Role.builder().id(2L).authority("USER").build();

        final var codes = service.codigosDo(userWith(user, financeiro));

        assertThat(codes).containsExactly("FINANCAS");
        verify(moduleRepository, never()).findAll();
    }

    @Test
    @DisplayName("só USER, sem role de módulo, não tem nenhum módulo")
    void userWithoutModuleRoleHasNone() {
        final var codes = service.codigosDo(userWith(Role.builder().id(2L).authority("USER").build()));

        assertThat(codes).isEmpty();
    }

    @Test
    @DisplayName("duas roles do mesmo módulo não duplicam o código")
    void duplicatedModuleCollapses() {
        final var financas = module(1, "FINANCAS");
        final var a = Role.builder().id(3L).authority("CONTADOR").module(financas).build();
        final var b = Role.builder().id(4L).authority("ANALISTA").module(financas).build();

        assertThat(service.codigosDo(userWith(a, b))).containsExactly("FINANCAS");
    }

    @Test
    @DisplayName("role excluída logicamente não concede módulo")
    void softDeletedRoleGrantsNothing() {
        final var role = Role.builder().id(3L).authority("CONTADOR").module(module(1, "FINANCAS")).build();
        role.setDeletedAt(java.time.LocalDateTime.now());

        assertThat(service.codigosDo(userWith(role))).isEmpty();
    }
}
