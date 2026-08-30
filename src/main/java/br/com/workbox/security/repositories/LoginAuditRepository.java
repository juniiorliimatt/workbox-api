package br.com.workbox.security.repositories;

import br.com.workbox.security.entities.LoginAudit;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

/**
 * @author CLAUDE-CODE
 * @author Junior Lima - oojuniin@outlook.com
 * @since 29-08-2026
 */

@Repository
public interface LoginAuditRepository extends JpaRepository<LoginAudit, UUID>, JpaSpecificationExecutor<LoginAudit> { }
