package br.com.workbox.security.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * @author CLAUDE-CODE
 * @author Junior Lima - oojuniin@outlook.com
 * @since 29-08-2026
 */

public record MfaCodeDTO(
        @NotBlank(message = "{validacao.codigoObrigatorio}")
        @Pattern(regexp = "\\d{6}", message = "{validacao.codigoDeveTer6Digitos}") String code) {
}
