package br.com.workbox.security.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.AdditionalAnswers.returnsFirstArg;

import br.com.workbox.exceptions.ResourceNotFoundException;
import br.com.workbox.security.dto.RoleModuleDTO;
import br.com.workbox.security.entities.AppModule;
import br.com.workbox.security.entities.Role;
import br.com.workbox.security.repositories.ModuleRepository;
import br.com.workbox.security.repositories.RoleRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.MessageSourceAccessor;
import org.springframework.context.support.ResourceBundleMessageSource;

class RoleServiceTest {

    private RoleRepository roleRepository;
    private ModuleRepository moduleRepository;
    private RoleService service;

    @BeforeEach
    void setUp() {
        roleRepository = mock(RoleRepository.class);
        moduleRepository = mock(ModuleRepository.class);
        final var messageSource = new ResourceBundleMessageSource();
        messageSource.setBasename("messages");
        messageSource.setDefaultEncoding("UTF-8");
        final var messages = new MessageSourceAccessor(messageSource, java.util.Locale.of("pt", "BR"));
        service = new RoleService(roleRepository, moduleRepository, messages);
        when(roleRepository.save(any(Role.class))).then(returnsFirstArg());
    }

    @Test
    @DisplayName("vincula a role ao módulo e devolve o módulo no DTO")
    void linksRoleToModule() {
        final var role = Role.builder().id(3L).authority("CONTADOR").build();
        final var financas = AppModule.builder().id(1L).code("FINANCAS").name("Finanças").build();
        when(roleRepository.findById(3L)).thenReturn(Optional.of(role));
        when(moduleRepository.findById(1L)).thenReturn(Optional.of(financas));

        final var dto = service.vincularModulo(3L, new RoleModuleDTO(1L));

        assertThat(role.getModule()).isSameAs(financas);
        assertThat(dto.module().code()).isEqualTo("FINANCAS");
    }

    @Test
    @DisplayName("moduleId nulo desvincula a role")
    void nullModuleUnlinks() {
        final var financas = AppModule.builder().id(1L).code("FINANCAS").name("Finanças").build();
        final var role = Role.builder().id(3L).authority("CONTADOR").module(financas).build();
        when(roleRepository.findById(3L)).thenReturn(Optional.of(role));

        final var dto = service.vincularModulo(3L, new RoleModuleDTO(null));

        assertThat(role.getModule()).isNull();
        assertThat(dto.module()).isNull();
        verify(moduleRepository, never()).findById(any());
    }

    @Test
    @DisplayName("role inexistente lança ResourceNotFoundException")
    void unknownRoleFails() {
        when(roleRepository.findById(9L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.vincularModulo(9L, new RoleModuleDTO(1L)))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(roleRepository, never()).save(any());
    }

    @Test
    @DisplayName("módulo inexistente lança ResourceNotFoundException e não grava")
    void unknownModuleFails() {
        when(roleRepository.findById(3L)).thenReturn(Optional.of(Role.builder().id(3L).authority("CONTADOR").build()));
        when(moduleRepository.findById(9L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.vincularModulo(3L, new RoleModuleDTO(9L)))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(roleRepository, never()).save(any());
    }

    @Test
    @DisplayName("ADMIN e USER não podem ser vinculados a módulo")
    void systemRolesCannotBeLinked() {
        when(roleRepository.findById(1L)).thenReturn(Optional.of(Role.builder().id(1L).authority("ADMIN").build()));

        assertThatThrownBy(() -> service.vincularModulo(1L, new RoleModuleDTO(1L)))
                .isInstanceOf(br.com.workbox.exceptions.InvalidRequestException.class);
        verify(roleRepository, never()).save(any());
    }
}
