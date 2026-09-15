package br.com.workbox.security.services;

import br.com.workbox.security.entities.ApiClientGrantType;
import br.com.workbox.security.repositories.ApiClientRepository;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * {@code UserDetailsService} do {@code introspectionFilterChain} (ver
 * {@link br.com.workbox.security.config.SecurityConfig}) — autentica clientes de
 * serviço-a-serviço (ex.: budget-service) via HTTP Basic contra a tabela
 * {@code api_clients}, não contra um cliente hardcoded em Java. Liberar um cliente novo
 * pro grant {@code CLIENT_SECRET} é inserir uma linha (Liquibase), nunca alterar esta
 * classe.
 *
 * @author CLAUDE-CODE
 * @author Junior Lima - oojuniin@outlook.com
 * @since 06-09-2026
 */

@Service
public class ApiClientUserDetailsService implements UserDetailsService {

    private final ApiClientRepository apiClientRepository;

    public ApiClientUserDetailsService(final ApiClientRepository apiClientRepository) {
        this.apiClientRepository = apiClientRepository;
    }

    @Override
    public UserDetails loadUserByUsername(final String clientId) throws UsernameNotFoundException {
        final var client = apiClientRepository.findByClientIdAndActiveTrue(clientId)
                .filter(c -> c.getAllowedGrantTypes().contains(ApiClientGrantType.CLIENT_SECRET))
                .orElseThrow(() -> new UsernameNotFoundException("Unknown, inactive or unauthorized client: " + clientId));

        return User.withUsername(client.getClientId())
                .password(client.getClientSecretHash())
                .authorities("INTROSPECTION_CLIENT")
                .build();
    }
}
