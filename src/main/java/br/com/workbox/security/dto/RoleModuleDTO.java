package br.com.workbox.security.dto;

/**
 * Corpo de {@code PUT /api/v1/role/{id}/module}. {@code moduleId} nulo desvincula a role
 * de qualquer módulo.
 *
 * @author CLAUDE-CODE
 * @author Junior Lima - oojuniin@outlook.com
 * @since 03-10-2026
 */

public record RoleModuleDTO(Long moduleId) { }
