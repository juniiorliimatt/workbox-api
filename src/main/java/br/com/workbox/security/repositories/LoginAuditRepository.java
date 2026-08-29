package br.com.workbox.security.repositories;

import br.com.workbox.security.entities.LoginAudit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

/**
 * @author CLAUDE-CODE
 * @author Junior Lima - oojuniin@outlook.com
 * @since 29-08-2026
 */

@Repository
public interface LoginAuditRepository extends JpaRepository<LoginAudit, UUID> { }
