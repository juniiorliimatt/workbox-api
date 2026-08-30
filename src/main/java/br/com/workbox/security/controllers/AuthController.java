package br.com.workbox.security.controllers;

import br.com.workbox.security.dto.ChangePasswordDTO;
import br.com.workbox.security.dto.ForgotPasswordDTO;
import br.com.workbox.security.dto.MfaCodeDTO;
import br.com.workbox.security.dto.MfaLoginDTO;
import br.com.workbox.security.dto.ResetPasswordDTO;
import br.com.workbox.security.dto.UserApiDTO;
import br.com.workbox.security.dto.UserApiLoginCredentialsDTO;
import br.com.workbox.security.dto.UserApiRegisterDTO;
import br.com.workbox.security.entities.UserApi;
import br.com.workbox.security.services.AvatarService;
import br.com.workbox.security.services.JwtService;
import br.com.workbox.security.services.LoginAuditService;
import br.com.workbox.security.services.LoginRateLimiter;
import br.com.workbox.security.services.MfaService;
import br.com.workbox.security.services.PasswordResetService;
import br.com.workbox.security.services.UserApiService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

/**
 * @author CLAUDE-CODE
 * @author Junior Lima - oojuniin@outlook.com
 * @since 29-08-2026
 */

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final JwtService jwtService;
    private final UserApiService userApiService;
    private final LoginAuditService loginAuditService;
    private final LoginRateLimiter loginRateLimiter;
    private final PasswordResetService passwordResetService;
    private final MfaService mfaService;
    private final AvatarService avatarService;

    public AuthController(final JwtService jwtService,
                           final UserApiService userApiService,
                           final LoginAuditService loginAuditService,
                           final LoginRateLimiter loginRateLimiter,
                           final PasswordResetService passwordResetService,
                           final MfaService mfaService,
                           final AvatarService avatarService) {
        this.jwtService = jwtService;
        this.userApiService = userApiService;
        this.loginAuditService = loginAuditService;
        this.loginRateLimiter = loginRateLimiter;
        this.passwordResetService = passwordResetService;
        this.mfaService = mfaService;
        this.avatarService = avatarService;
    }

    /** Auto-cadastro público — sempre USER, nunca aceita roles do payload (ver {@link UserApiService#register}). */
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody @Valid UserApiRegisterDTO dto, HttpServletRequest request) {
        if (!loginRateLimiter.isAllowed("register:" + clientIp(request))) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(ProblemDetail.forStatusAndDetail(HttpStatus.TOO_MANY_REQUESTS, "Too many registration attempts, try again later"));
        }
        final var created = userApiService.register(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody UserApiLoginCredentialsDTO dto, HttpServletRequest request) {
        final var ip = clientIp(request);
        if (!loginRateLimiter.isAllowed("login:" + ip)) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(ProblemDetail.forStatusAndDetail(HttpStatus.TOO_MANY_REQUESTS, "Too many login attempts, try again later"));
        }

        final var result = userApiService.attemptLogin(dto.email(), dto.password());
        loginAuditService.record(dto.email(), result.success(), result.failureReason(), ip);

        if (!result.success()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, "Invalid username or password"));
        }

        final var user = result.user();
        if (Boolean.TRUE.equals(user.getMfaEnabled())) {
            return ResponseEntity.ok(Map.of("mfa_required", true, "mfa_token", jwtService.issueMfaChallengeToken(user)));
        }
        return ResponseEntity.ok(issueTokenPair(user));
    }

    /**
     * Segunda etapa do login quando a conta tem MFA habilitado: troca o
     * {@code mfa_token} (válido por 5min, prova só que usuário+senha bateram) por um
     * código TOTP válido para finalmente emitir access+refresh.
     */
    @PostMapping("/mfa/login")
    public ResponseEntity<?> mfaLogin(@RequestBody @Valid MfaLoginDTO dto, HttpServletRequest request) {
        if (!loginRateLimiter.isAllowed("mfa-login:" + clientIp(request))) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(ProblemDetail.forStatusAndDetail(HttpStatus.TOO_MANY_REQUESTS, "Too many attempts, try again later"));
        }

        final var user = jwtService.validateMfaChallengeToken(dto.mfaToken());
        if (!mfaService.verifyCode(user, dto.code())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, "Invalid MFA code"));
        }
        return ResponseEntity.ok(issueTokenPair(user));
    }

    @PostMapping("/mfa/enroll")
    public ResponseEntity<?> enrollMfa(Authentication authentication) {
        final var user = (UserApi) userApiService.loadUserByUsername(authentication.getName());
        return ResponseEntity.ok(mfaService.enroll(user));
    }

    /** Confirma o primeiro código TOTP e só então habilita MFA na conta. */
    @PostMapping("/mfa/verify")
    public ResponseEntity<Void> verifyMfa(Authentication authentication, @RequestBody @Valid MfaCodeDTO dto) {
        final var user = (UserApi) userApiService.loadUserByUsername(authentication.getName());
        mfaService.verifyAndEnable(user, dto.code());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/mfa/disable")
    public ResponseEntity<Void> disableMfa(Authentication authentication, @RequestBody @Valid MfaCodeDTO dto) {
        final var user = (UserApi) userApiService.loadUserByUsername(authentication.getName());
        mfaService.disable(user, dto.code());
        return ResponseEntity.noContent().build();
    }

    private Map<String, String> issueTokenPair(UserApi user) {
        Map<String, String> tokens = new HashMap<>();
        tokens.put("access_token", jwtService.generateToken(user));
        tokens.put("refresh_token", jwtService.issueRefreshToken(user));
        return tokens;
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

    /** Upload/troca do próprio avatar — reencodado e validado em {@link AvatarService#store}. */
    @PostMapping(value = "/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Void> uploadAvatar(Authentication authentication, @RequestParam("file") MultipartFile file) {
        final var user = (UserApi) userApiService.loadUserByUsername(authentication.getName());
        avatarService.store(user.getId(), file);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/avatar")
    public ResponseEntity<Void> deleteAvatar(Authentication authentication) {
        final var user = (UserApi) userApiService.loadUserByUsername(authentication.getName());
        avatarService.delete(user.getId());
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
