package br.com.workbox.security.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import br.com.workbox.security.entities.ApiClient;
import br.com.workbox.security.entities.ApiClientGrantType;
import br.com.workbox.security.repositories.ApiClientRepository;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

class ApiClientUserDetailsServiceTest {

    private ApiClientRepository apiClientRepository;
    private ApiClientUserDetailsService service;

    @BeforeEach
    void setUp() {
        apiClientRepository = mock(ApiClientRepository.class);
        service = new ApiClientUserDetailsService(apiClientRepository);
    }

    private ApiClient client(final Set<ApiClientGrantType> grantTypes) {
        return ApiClient.builder()
                .clientId("budget-service")
                .clientSecretHash("hash")
                .allowedGrantTypes(grantTypes)
                .active(true)
                .build();
    }

    @Test
    @DisplayName("cliente ativo com grant CLIENT_SECRET retorna UserDetails com o hash armazenado")
    void activeClientWithGrantReturnsUserDetails() {
        when(apiClientRepository.findByClientIdAndActiveTrue("budget-service"))
                .thenReturn(Optional.of(client(Set.of(ApiClientGrantType.CLIENT_SECRET))));

        final var userDetails = service.loadUserByUsername("budget-service");

        assertThat(userDetails.getUsername()).isEqualTo("budget-service");
        assertThat(userDetails.getPassword()).isEqualTo("hash");
    }

    @Test
    @DisplayName("cliente sem o grant CLIENT_SECRET não autentica mesmo ativo")
    void clientWithoutGrantIsRejected() {
        when(apiClientRepository.findByClientIdAndActiveTrue("budget-service"))
                .thenReturn(Optional.of(client(Set.of())));

        assertThatThrownBy(() -> service.loadUserByUsername("budget-service"))
                .isInstanceOf(UsernameNotFoundException.class);
    }

    @Test
    @DisplayName("client_id desconhecido ou inativo não autentica")
    void unknownOrInactiveClientIsRejected() {
        when(apiClientRepository.findByClientIdAndActiveTrue("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.loadUserByUsername("ghost"))
                .isInstanceOf(UsernameNotFoundException.class);
    }
}
