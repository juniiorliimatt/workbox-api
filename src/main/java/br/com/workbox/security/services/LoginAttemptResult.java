package br.com.workbox.security.services;

import br.com.workbox.security.entities.UserApi;

/**
 * @author CLAUDE-CODE
 * @author Junior Lima - oojuniin@outlook.com
 * @since 29-08-2026
 */

public record LoginAttemptResult(boolean success, UserApi user, String failureReason) {

    public static LoginAttemptResult success(final UserApi user) {
        return new LoginAttemptResult(true, user, null);
    }

    public static LoginAttemptResult failure(final String reason) {
        return new LoginAttemptResult(false, null, reason);
    }
}
