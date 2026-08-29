package br.com.workbox.security.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record MfaLoginDTO(
        @NotBlank(message = "MFA token is mandatory") String mfaToken,
        @NotBlank(message = "Code is mandatory")
        @Pattern(regexp = "\\d{6}", message = "Code must be 6 digits") String code) {
}
