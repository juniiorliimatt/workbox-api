package br.com.workbox.config.audit;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.envers.DefaultRevisionEntity;
import org.hibernate.envers.RevisionEntity;

/**
 * Revisão do Envers com quem fez a mudança — {@link RevisionListenerImpl} preenche
 * {@code username} a partir do SecurityContext, mesma fonte que {@code AuditorAwareImpl}
 * usa pra createdBy/updatedBy.
 *
 * @author CLAUDE-CODE
 * @author Junior Lima - oojuniin@outlook.com
 * @since 29-08-2026
 */
@Getter
@Setter
@Entity
@Table(name = "rev_info")
@RevisionEntity(RevisionListenerImpl.class)
public class CustomRevisionEntity extends DefaultRevisionEntity {

    @Column(nullable = false)
    private String username;
}
