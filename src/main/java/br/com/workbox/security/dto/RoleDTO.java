package br.com.workbox.security.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * @author CLAUDE-CODE
 * @author Junior Lima - oojuniin@outlook.com
 * @since 29-08-2026
 */

public record RoleDTO(Long id, @NotBlank(message = "{validacao.authorityObrigatoria}") String authority) { }
