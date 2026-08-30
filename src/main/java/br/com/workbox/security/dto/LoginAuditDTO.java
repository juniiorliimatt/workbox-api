package br.com.workbox.security.dto;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * @author CLAUDE-CODE
 * @author Junior Lima - oojuniin@outlook.com
 * @since 30-08-2026
 */

public record LoginAuditDTO(UUID id,
                             String email,
                             boolean successful,
                             String reason,
                             String ipAddress,
                             LocalDateTime createdAt) { }
