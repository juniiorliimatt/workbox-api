package br.com.workbox.security.dto;

/**
 * {@code otpAuthUri} é o {@code otpauth://} completo — renderizar o QR code (se for o
 * caso) é responsabilidade do client, não desta API.
 *
 * @author CLAUDE-CODE
 * @author Junior Lima - oojuniin@outlook.com
 * @since 29-08-2026
 */
public record MfaEnrollResponseDTO(String secret, String otpAuthUri) {
}
