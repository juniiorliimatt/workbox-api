package br.com.workbox.security.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.workbox.exceptions.ResourceNotFoundException;
import br.com.workbox.security.dto.UserApiInsertOrUpdateDTO;
import br.com.workbox.security.entities.Role;
import br.com.workbox.security.entities.UserApi;
import br.com.workbox.security.repositories.RoleRepository;
import br.com.workbox.security.repositories.UserApiRepository;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class UserApiServiceTest {

    private static final String USERNAME = "alice";
    private static final String RAW_PASSWORD = "S3nh@Forte!";
    private static final String HASH = "$2a$12$hashedvalue";

    @Mock
    private UserApiRepository userApiRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private UserApiService service;

    @BeforeEach
    void setUp() {
        service = new UserApiService(userApiRepository, roleRepository, passwordEncoder);
    }

    private UserApi.UserApiBuilder aUser() {
        return UserApi.builder()
                .id(UUID.randomUUID())
                .username(USERNAME)
                .password(HASH)
                .isEnabled(true)
                .isAccountNonExpired(true)
                .isAccountNonLocked(true)
                .isCredentialsNonExpired(true)
                .roles(Set.of());
    }

    @Nested
    @DisplayName("loadUserByUsername")
    class LoadUserByUsername {

        @Test
        @DisplayName("retorna a entidade quando o usuário existe")
        void returnsUserWhenFound() {
            final var user = aUser().build();
            when(userApiRepository.findByUsername(USERNAME)).thenReturn(Optional.of(user));

            final var result = service.loadUserByUsername(USERNAME);

            assertThat(result).isSameAs(user);
        }

        @Test
        @DisplayName("lança UsernameNotFoundException quando não existe")
        void throwsWhenNotFound() {
            when(userApiRepository.findByUsername("ghost")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.loadUserByUsername("ghost"))
                    .isInstanceOf(UsernameNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("validateCredentials")
    class ValidateCredentials {

        @Test
        @DisplayName("true quando usuário habilitado e senha confere")
        void trueForEnabledUserWithMatchingPassword() {
            final var user = aUser().build();
            when(userApiRepository.findByUsername(USERNAME)).thenReturn(Optional.of(user));
            when(passwordEncoder.matches(RAW_PASSWORD, HASH)).thenReturn(true);

            assertThat(service.validateCredentials(USERNAME, RAW_PASSWORD)).isTrue();
        }

        @Test
        @DisplayName("false quando a senha não confere")
        void falseForWrongPassword() {
            final var user = aUser().build();
            when(userApiRepository.findByUsername(USERNAME)).thenReturn(Optional.of(user));
            when(passwordEncoder.matches("senha-errada", HASH)).thenReturn(false);

            assertThat(service.validateCredentials(USERNAME, "senha-errada")).isFalse();
        }

        @Test
        @DisplayName("false quando o usuário não existe — sem lançar exceção (evita enumeração)")
        void falseForUnknownUserWithoutThrowing() {
            when(userApiRepository.findByUsername("ghost")).thenReturn(Optional.empty());

            assertThat(service.validateCredentials("ghost", "qualquer")).isFalse();
        }

        @Test
        @DisplayName("false quando a conta está desabilitada, mesmo com senha correta")
        void falseForDisabledAccount() {
            final var user = aUser().isEnabled(false).build();
            when(userApiRepository.findByUsername(USERNAME)).thenReturn(Optional.of(user));

            assertThat(service.validateCredentials(USERNAME, RAW_PASSWORD)).isFalse();
            verify(passwordEncoder, never()).matches(any(), any());
        }

        @Test
        @DisplayName("false quando a conta está bloqueada (isAccountNonLocked=false)")
        void falseForLockedAccount() {
            final var user = aUser().isAccountNonLocked(false).build();
            when(userApiRepository.findByUsername(USERNAME)).thenReturn(Optional.of(user));

            assertThat(service.validateCredentials(USERNAME, RAW_PASSWORD)).isFalse();
            verify(passwordEncoder, never()).matches(any(), any());
        }

        @Test
        @DisplayName("false quando a conta está expirada (isAccountNonExpired=false)")
        void falseForExpiredAccount() {
            final var user = aUser().isAccountNonExpired(false).build();
            when(userApiRepository.findByUsername(USERNAME)).thenReturn(Optional.of(user));

            assertThat(service.validateCredentials(USERNAME, RAW_PASSWORD)).isFalse();
        }

        @Test
        @DisplayName("false quando as credenciais estão expiradas (isCredentialsNonExpired=false)")
        void falseForExpiredCredentials() {
            final var user = aUser().isCredentialsNonExpired(false).build();
            when(userApiRepository.findByUsername(USERNAME)).thenReturn(Optional.of(user));

            assertThat(service.validateCredentials(USERNAME, RAW_PASSWORD)).isFalse();
        }
    }

    @Nested
    @DisplayName("findAll / findById")
    class Queries {

        @Test
        @DisplayName("findAll(Pageable) mapeia entidades pra DTO preservando paginação")
        void findAllPageable() {
            final var user = aUser().build();
            final var page = new PageImpl<>(List.of(user), PageRequest.of(0, 10), 1);
            when(userApiRepository.findAll(PageRequest.of(0, 10))).thenReturn(page);

            final var result = service.findAll(PageRequest.of(0, 10));

            assertThat(result.getTotalElements()).isEqualTo(1);
            assertThat(result.getContent().get(0).getUsername()).isEqualTo(USERNAME);
        }

        @Test
        @DisplayName("findById lança ResourceNotFoundException quando não existe")
        void findByIdNotFound() {
            final var id = UUID.randomUUID();
            when(userApiRepository.findById(id)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.findById(id))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("findById retorna o DTO quando existe")
        void findByIdFound() {
            final var user = aUser().build();
            when(userApiRepository.findById(user.getId())).thenReturn(Optional.of(user));

            final var result = service.findById(user.getId());

            assertThat(result.getUsername()).isEqualTo(USERNAME);
            assertThat(result.isEnabled()).isTrue();
        }
    }

    @Nested
    @DisplayName("save / update / delete")
    class Mutations {

        @Test
        @DisplayName("save resolve as roles, faz hash da senha e persiste")
        void saveHashesPasswordAndResolvesRoles() {
            final var role = Role.builder().id(1L).authority("USER").build();
            final var dto = new UserApiInsertOrUpdateDTO(null, USERNAME, RAW_PASSWORD, true, Set.of(role));
            when(roleRepository.findById(1L)).thenReturn(Optional.of(role));
            when(passwordEncoder.encode(RAW_PASSWORD)).thenReturn(HASH);
            when(userApiRepository.save(any(UserApi.class))).thenAnswer(inv -> inv.getArgument(0));

            final var result = service.save(dto);

            assertThat(result.getUsername()).isEqualTo(USERNAME);
            verify(userApiRepository).save(argThatPasswordIsHashed());
        }

        @Test
        @DisplayName("save lança ResourceNotFoundException quando uma role não existe")
        void saveThrowsWhenRoleMissing() {
            final var role = Role.builder().id(99L).authority("GHOST").build();
            final var dto = new UserApiInsertOrUpdateDTO(null, USERNAME, RAW_PASSWORD, true, Set.of(role));
            when(roleRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.save(dto))
                    .isInstanceOf(ResourceNotFoundException.class);
            verify(userApiRepository, never()).save(any());
        }

        @Test
        @DisplayName("update aplica as mudanças e re-hasheia a senha")
        void updateRehashesPassword() {
            final var user = aUser().build();
            final var dto = new UserApiInsertOrUpdateDTO(user.getId(), "bob12", "nova-senha", false, Set.of());
            when(userApiRepository.findById(user.getId())).thenReturn(Optional.of(user));
            when(passwordEncoder.encode("nova-senha")).thenReturn("novo-hash");
            when(userApiRepository.save(user)).thenReturn(user);

            final var result = service.update(dto);

            assertThat(user.getUsername()).isEqualTo("bob12");
            assertThat(user.getPassword()).isEqualTo("novo-hash");
            assertThat(user.getIsEnabled()).isFalse();
            assertThat(result.isEnabled()).isFalse();
        }

        @Test
        @DisplayName("delete lança ResourceNotFoundException quando o usuário não existe")
        void deleteThrowsWhenMissing() {
            final var id = UUID.randomUUID();
            when(userApiRepository.findById(id)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.delete(id))
                    .isInstanceOf(ResourceNotFoundException.class);
            verify(userApiRepository, never()).deleteById(any());
        }

        @Test
        @DisplayName("delete remove pelo id quando o usuário existe")
        void deleteRemovesExistingUser() {
            final var user = aUser().build();
            when(userApiRepository.findById(user.getId())).thenReturn(Optional.of(user));

            service.delete(user.getId());

            verify(userApiRepository, times(1)).deleteById(eq(user.getId()));
        }

        private UserApi argThatPasswordIsHashed() {
            return org.mockito.ArgumentMatchers.argThat(u -> HASH.equals(u.getPassword()));
        }
    }
}
