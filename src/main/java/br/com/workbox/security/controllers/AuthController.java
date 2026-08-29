package br.com.workbox.security.controllers;

import br.com.workbox.security.dto.ChangePasswordDTO;
import br.com.workbox.security.dto.ForgotPasswordDTO;
import br.com.workbox.security.dto.ResetPasswordDTO;
import br.com.workbox.security.dto.UserApiDTO;
import br.com.workbox.security.dto.UserApiLoginCredentialsDTO;
import br.com.workbox.security.services.JwtService;
import br.com.workbox.security.services.LoginAuditService;
import br.com.workbox.security.services.LoginRateLimiter;
import br.com.workbox.security.services.PasswordResetService;
import br.com.workbox.security.services.UserApiService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final JwtService jwtService;
    private final UserApiService userApiService;
    private final LoginAuditService loginAuditService;
    private final LoginRateLimiter loginRateLimiter;
    private final PasswordResetService passwordResetService;

    public AuthController(final JwtService jwtService,
                           final UserApiService userApiService,
                           final LoginAuditService loginAuditService,
                           final LoginRateLimiter loginRateLimiter,
                           final PasswordResetService passwordResetService) {
        this.jwtService = jwtService;
        this.userApiService = userApiService;
        this.loginAuditService = loginAuditService;
        this.loginRateLimiter = loginRateLimiter;
        this.passwordResetService = passwordResetService;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody UserApiLoginCredentialsDTO dto, HttpServletRequest request) {
        final var ip = clientIp(request);
        if (!loginRateLimiter.isAllowed("login:" + ip)) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(ProblemDetail.forStatusAndDetail(HttpStatus.TOO_MANY_REQUESTS, "Too many login attempts, try again later"));
        }

        final var result = userApiService.attemptLogin(dto.username(), dto.password());
        loginAuditService.record(dto.username(), result.success(), result.failureReason(), ip);

        if (!result.success()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, "Invalid username or password"));
        }

        final var user = result.user();
        Map<String, String> tokens = new HashMap<>();
        tokens.put("access_token", jwtService.generateToken(user));
        tokens.put("refresh_token", jwtService.issueRefreshToken(user));
        return ResponseEntity.ok(tokens);
    }

    /**
     * Refresh token é de uso único (rotação): cada chamada aqui revoga o token
     * apresentado e devolve um novo par access+refresh. Reapresentar um refresh token já
     * usado é tratado como reuso de token roubado — ver
     * {@link JwtService#rotateRefreshToken}.
     */
    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(@RequestParam String refreshToken, HttpServletRequest request) {
        if (!loginRateLimiter.isAllowed("refresh:" + clientIp(request))) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(ProblemDetail.forStatusAndDetail(HttpStatus.TOO_MANY_REQUESTS, "Too many refresh attempts, try again later"));
        }

        final var rotated = jwtService.rotateRefreshToken(refreshToken);
        Map<String, String> tokens = new HashMap<>();
        tokens.put("access_token", rotated.accessToken());
        tokens.put("refresh_token", rotated.refreshToken());
        return ResponseEntity.ok(tokens);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(Authentication authentication) {
        userApiService.logout(authentication.getName());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    public ResponseEntity<UserApiDTO> me(Authentication authentication) {
        return ResponseEntity.ok(userApiService.me(authentication.getName()));
    }

    @PutMapping("/password")
    public ResponseEntity<Void> changePassword(Authentication authentication, @RequestBody @Valid ChangePasswordDTO dto) {
        userApiService.changePassword(authentication.getName(), dto);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody @Valid ForgotPasswordDTO dto) {
        // Chave por e-mail (não IP): um atacante rotacionando IP não ganha tentativas
        // extras contra o mesmo endereço.
        if (!loginRateLimiter.isAllowed("forgot-password:" + dto.email().toLowerCase())) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(ProblemDetail.forStatusAndDetail(HttpStatus.TOO_MANY_REQUESTS, "Too many password reset requests, try again later"));
        }
        // Sempre 204, exista ou não o e-mail — não revelar quais e-mails têm conta.
        passwordResetService.requestReset(dto.email());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Void> resetPassword(@RequestBody @Valid ResetPasswordDTO dto) {
        passwordResetService.resetPassword(dto.token(), dto.newPassword());
        return ResponseEntity.noContent().build();
    }

    private String clientIp(HttpServletRequest request) {
        final var forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
