package br.com.workbox.security.dto;

import java.time.LocalDateTime;

/**
 * Uma revisão do histórico de {@code Role} (Hibernate Envers) — ver {@link UserApiRevisionDTO}.
 *
 * @author CLAUDE-CODE
 * @author Junior Lima - oojuniin@outlook.com
 * @since 30-08-2026
 */

public record RoleRevisionDTO(int revision,
                               LocalDateTime changedAt,
                               String changedBy,
                               String revisionType,
                               Long id,
                               String authority,
                               LocalDateTime deletedAt) { }
