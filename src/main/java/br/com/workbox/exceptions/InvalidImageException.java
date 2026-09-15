package br.com.workbox.exceptions;

/**
 * @author CLAUDE-CODE
 * @author Junior Lima - oojuniin@outlook.com
 * @since 30-08-2026
 */

public class InvalidImageException extends RuntimeException {
    public InvalidImageException(final String message) {
        super(message);
    }
    public InvalidImageException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
