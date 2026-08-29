package br.com.workbox.security.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * @author CLAUDE-CODE
 * @author Junior Lima - oojuniin@outlook.com
 * @since 29-08-2026
 */

public record MfaLoginDTO(
        @NotBlank(message = "MFA token is mandatory") String mfaToken,
        @NotBlank(message = "Code is mandatory")
        @Pattern(regexp = "\\d{6}", message = "Code must be 6 digits") String code) {
}
