package br.com.workbox.config.audit;

import org.hibernate.envers.RevisionListener;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.jwt.Jwt;

/**
 * Instanciado pelo Hibernate, não pelo Spring — sem DI, por isso lê o SecurityContext
 * estaticamente em vez de injetar um AuditorAware.
 *
 * @author CLAUDE-CODE
 * @author Junior Lima - oojuniin@outlook.com
 * @since 29-08-2026
 */
public class RevisionListenerImpl implements RevisionListener {

    @Override
    public void newRevision(final Object revisionEntity) {
        final var revision = (CustomRevisionEntity) revisionEntity;
        revision.setUsername(currentUsername());
    }

    private String currentUsername() {
        final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return "system";
        }
        final Object principal = authentication.getPrincipal();
        if (principal instanceof Jwt jwt) {
            return jwt.getClaimAsString("sub");
        }
        if (principal instanceof UserDetails userDetails) {
            return userDetails.getUsername();
        }
        return principal.toString();
    }
}
