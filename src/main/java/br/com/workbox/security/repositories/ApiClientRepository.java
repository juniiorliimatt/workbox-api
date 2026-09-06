package br.com.workbox.security.repositories;

import br.com.workbox.security.entities.ApiClient;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * @author CLAUDE-CODE
 * @author Junior Lima - oojuniin@outlook.com
 * @since 06-09-2026
 */

@Repository
public interface ApiClientRepository extends JpaRepository<ApiClient, UUID> {

    Optional<ApiClient> findByClientIdAndActiveTrue(String clientId);

}
