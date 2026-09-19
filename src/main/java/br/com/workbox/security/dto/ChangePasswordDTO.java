package br.com.workbox.security.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * @author CLAUDE-CODE
 * @author Junior Lima - oojuniin@outlook.com
 * @since 29-08-2026
 */

public record ChangePasswordDTO(
        @NotBlank(message = "{validacao.senhaAtualObrigatoria}") String currentPassword,
        @NotBlank(message = "{validacao.novaSenhaObrigatoria}") @Size(min = 8, max = 100) String newPassword) { }
