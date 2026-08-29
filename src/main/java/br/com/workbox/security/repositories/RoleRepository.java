package br.com.workbox.security.repositories;

import br.com.workbox.security.entities.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * @author CLAUDE-CODE
 * @author Junior Lima - oojuniin@outlook.com
 * @since 29-08-2026
 */

@Repository
public interface RoleRepository extends JpaRepository<Role, Long> { }
