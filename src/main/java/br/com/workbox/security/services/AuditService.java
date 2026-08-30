package br.com.workbox.security.services;

import br.com.workbox.config.audit.CustomRevisionEntity;
import br.com.workbox.security.dto.LoginAuditDTO;
import br.com.workbox.security.dto.RoleRevisionDTO;
import br.com.workbox.security.dto.UserApiRevisionDTO;
import br.com.workbox.security.entities.LoginAudit;
import br.com.workbox.security.entities.Role;
import br.com.workbox.security.entities.UserApi;
import br.com.workbox.security.repositories.LoginAuditRepository;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.envers.RevisionType;
import org.hibernate.envers.query.AuditEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Leitura das trilhas de auditoria já gravadas por {@link LoginAuditService} (tentativas
 * de login) e pelo Hibernate Envers (histórico de {@link UserApi}/{@link Role}) — este
 * serviço não grava nada, só expõe pra consulta administrativa.
 *
 * @author CLAUDE-CODE
 * @author Junior Lima - oojuniin@outlook.com
 * @since 30-08-2026
 */
@Service
public class AuditService {

    private final EntityManager entityManager;
    private final LoginAuditRepository loginAuditRepository;

    public AuditService(final EntityManager entityManager, final LoginAuditRepository loginAuditRepository) {
        this.entityManager = entityManager;
        this.loginAuditRepository = loginAuditRepository;
    }

    /**
     * Filtro dinâmico via Specification em vez de JPQL com {@code (:param IS NULL OR ...)}
     * — esse padrão quebra contra Postgres real ("could not determine data type of
     * parameter") quando o parâmetro vem nulo, porque o driver não consegue inferir o tipo
     * do bind a partir só de "? IS NULL". H2 (perfil de teste) não reproduz o problema, só
     * apareceu rodando contra Postgres de verdade — Specification só adiciona predicado
     * pro filtro que de fato veio preenchido, sem nunca bindar um parâmetro nulo.
     */
    @Transactional(readOnly = true)
    public Page<LoginAuditDTO> findLoginAudits(final String email, final LocalDateTime from, final LocalDateTime to, final Pageable pageable) {
        Specification<LoginAudit> spec = Specification.unrestricted();
        if (email != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("email"), email));
        }
        if (from != null) {
            spec = spec.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("createdAt"), from));
        }
        if (to != null) {
            spec = spec.and((root, query, cb) -> cb.lessThanOrEqualTo(root.get("createdAt"), to));
        }
        return loginAuditRepository.findAll(spec, pageable)
                .map(audit -> new LoginAuditDTO(audit.getId(), audit.getEmail(), audit.isSuccessful(), audit.getReason(), audit.getIpAddress(), audit.getCreatedAt()));
    }

    @Transactional(readOnly = true)
    @SuppressWarnings("unchecked")
    public List<UserApiRevisionDTO> findUserHistory(final java.util.UUID userId) {
        final var reader = AuditReaderFactory.get(entityManager);
        final List<Object[]> rows = reader.createQuery()
                .forRevisionsOfEntity(UserApi.class, false, true)
                .add(AuditEntity.id().eq(userId))
                .addOrder(AuditEntity.revisionNumber().asc())
                .getResultList();
        return rows.stream()
                .map(row -> {
                    final var snapshot = (UserApi) row[0];
                    final var revisionEntity = (CustomRevisionEntity) row[1];
                    final var revisionType = (RevisionType) row[2];
                    return new UserApiRevisionDTO(
                            revisionEntity.getId(),
                            toLocalDateTime(revisionEntity.getTimestamp()),
                            revisionEntity.getUsername(),
                            revisionType.name(),
                            snapshot.getId(),
                            snapshot.getSocialName(),
                            snapshot.getEmail(),
                            snapshot.getIsEnabled(),
                            snapshot.getMfaEnabled(),
                            snapshot.getDeletedAt());
                })
                .toList();
    }

    @Transactional(readOnly = true)
    @SuppressWarnings("unchecked")
    public List<RoleRevisionDTO> findRoleHistory(final Long roleId) {
        final var reader = AuditReaderFactory.get(entityManager);
        final List<Object[]> rows = reader.createQuery()
                .forRevisionsOfEntity(Role.class, false, true)
                .add(AuditEntity.id().eq(roleId))
                .addOrder(AuditEntity.revisionNumber().asc())
                .getResultList();
        return rows.stream()
                .map(row -> {
                    final var snapshot = (Role) row[0];
                    final var revisionEntity = (CustomRevisionEntity) row[1];
                    final var revisionType = (RevisionType) row[2];
                    return new RoleRevisionDTO(
                            revisionEntity.getId(),
                            toLocalDateTime(revisionEntity.getTimestamp()),
                            revisionEntity.getUsername(),
                            revisionType.name(),
                            snapshot.getId(),
                            snapshot.getAuthority(),
                            snapshot.getDeletedAt());
                })
                .toList();
    }

    private LocalDateTime toLocalDateTime(final long epochMillis) {
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(epochMillis), ZoneId.systemDefault());
    }
}
