package br.com.workbox.security.dto;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Uma revisão do histórico de {@code UserApi} (Hibernate Envers). {@code changedBy} vem
 * do {@code SecurityContext} no momento da mudança (email de quem alterou, ou "system"
 * fora de um contexto autenticado). {@code revisionType} é {@code ADD}/{@code MOD}/
 * {@code DEL} — na prática só ADD/MOD aparecem aqui, já que a exclusão de usuário é
 * lógica (campo {@code deletedAt}), não um DELETE físico.
 *
 * @author CLAUDE-CODE
 * @author Junior Lima - oojuniin@outlook.com
 * @since 30-08-2026
 */

public record UserApiRevisionDTO(int revision,
                                  LocalDateTime changedAt,
                                  String changedBy,
                                  String revisionType,
                                  UUID id,
                                  String socialName,
                                  String email,
                                  Boolean enabled,
                                  Boolean mfaEnabled,
                                  LocalDateTime deletedAt) { }
