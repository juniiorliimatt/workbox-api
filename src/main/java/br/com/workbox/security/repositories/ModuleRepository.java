package br.com.workbox.security.repositories;

import br.com.workbox.security.entities.AppModule;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * @author CLAUDE-CODE
 * @author Junior Lima - oojuniin@outlook.com
 * @since 03-10-2026
 */

@Repository
public interface ModuleRepository extends JpaRepository<AppModule, Long> {

    Optional<AppModule> findByCode(final String code);

}
