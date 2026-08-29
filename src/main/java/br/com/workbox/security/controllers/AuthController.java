package br.com.workbox.security.controllers;

import br.com.workbox.exceptions.InvalidRefreshTokenException;
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
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
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
    public ResponseEntity<Map<String, String>> login(@RequestBody UserApiLoginCredentialsDTO dto, HttpServletRequest request) {
        final var ip = clientIp(request);
        if (!loginRateLimiter.isAllowed(ip)) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(Map.of("error", "Too many login attempts, try again later"));
        }

        final var result = userApiService.attemptLogin(dto.username(), dto.password());
        loginAuditService.record(dto.username(), result.success(), result.failureReason(), ip);

        if (!result.success()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Invalid username or password"));
        }

        final var user = result.user();
        Map<String, String> tokens = new HashMap<>();
        tokens.put("access_token", jwtService.generateToken(user));
        tokens.put("refresh_token", jwtService.generateRefreshToken(user));
        return ResponseEntity.ok(tokens);
    }

    @PostMapping("/refresh")
    public ResponseEntity<Map<String, String>> refresh(@RequestParam String refreshToken) {
        try {
            final var user = jwtService.validateRefreshToken(refreshToken);
            Map<String, String> tokens = new HashMap<>();
            tokens.put("access_token", jwtService.generateToken(user));
            return ResponseEntity.ok(tokens);
        } catch (InvalidRefreshTokenException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Invalid or expired refresh token"));
        }
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
    public ResponseEntity<Void> forgotPassword(@RequestBody @Valid ForgotPasswordDTO dto) {
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
