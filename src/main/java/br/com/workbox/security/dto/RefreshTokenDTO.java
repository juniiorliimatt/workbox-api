package br.com.workbox.security.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * @author CLAUDE-CODE
 * @author Junior Lima - oojuniin@outlook.com
 * @since 30-08-2026
 */

public record RefreshTokenDTO(@NotBlank(message = "Refresh token is mandatory") String refreshToken) { }
