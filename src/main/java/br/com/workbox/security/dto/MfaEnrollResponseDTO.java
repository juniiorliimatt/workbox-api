package br.com.workbox.security.dto;

/**
 * {@code otpAuthUri} é o {@code otpauth://} completo — renderizar o QR code (se for o
 * caso) é responsabilidade do client, não desta API.
 */
public record MfaEnrollResponseDTO(String secret, String otpAuthUri) {
}
