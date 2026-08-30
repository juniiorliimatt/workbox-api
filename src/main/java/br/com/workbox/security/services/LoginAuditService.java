package br.com.workbox.security.services;

import br.com.workbox.security.entities.LoginAudit;
import br.com.workbox.security.repositories.LoginAuditRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author CLAUDE-CODE
 * @author Junior Lima - oojuniin@outlook.com
 * @since 29-08-2026
 */

@Service
public class LoginAuditService {

    private final LoginAuditRepository loginAuditRepository;

    public LoginAuditService(final LoginAuditRepository loginAuditRepository) {
        this.loginAuditRepository = loginAuditRepository;
    }

    // REQUIRES_NEW: auditoria é best-effort, nunca deve fazer o login falhar nem ficar
    // presa na mesma transação de um attemptLogin que der rollback por outro motivo.
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(final String email, final boolean successful, final String reason, final String ipAddress) {
        loginAuditRepository.save(LoginAudit.of(email, successful, reason, ipAddress));
    }
}
