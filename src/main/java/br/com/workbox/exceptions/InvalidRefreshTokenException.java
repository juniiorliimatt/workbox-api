package br.com.workbox.exceptions;

/**
 * @author CLAUDE-CODE
 * @author Junior Lima - oojuniin@outlook.com
 * @since 29-08-2026
 */

public class InvalidRefreshTokenException extends RuntimeException {

    public InvalidRefreshTokenException(final String message) {
        super(message);
    }

    public InvalidRefreshTokenException(final String message, final Throwable cause) {
        super(message, cause);
    }

}
