package br.com.workbox.security.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * @author CLAUDE-CODE
 * @author Junior Lima - oojuniin@outlook.com
 * @since 29-08-2026
 */

public record RoleDTO(Long id, @NotBlank(message = "{validacao.authorityObrigatoria}") String authority, ModuleDTO module) {

    /** Atalho sem módulo — usado em criação/atualização, onde o vínculo vai por {@code PUT /role/{id}/module}. */
    public RoleDTO(final Long id, final String authority) {
        this(id, authority, null);
    }
}
