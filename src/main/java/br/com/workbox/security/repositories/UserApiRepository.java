package br.com.workbox.security.repositories;

import br.com.workbox.security.entities.UserApi;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * @author CLAUDE-CODE
 * @author Junior Lima - oojuniin@outlook.com
 * @since 29-08-2026
 */

@Repository
public interface UserApiRepository extends JpaRepository<UserApi, UUID> {

    Optional<UserApi> findByUsername(final String username);

    Optional<UserApi> findByEmail(final String email);

}
