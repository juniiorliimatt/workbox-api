package br.com.workbox.security.services;

import br.com.workbox.exceptions.InvalidRefreshTokenException;
import br.com.workbox.exceptions.InvalidTokenException;
import br.com.workbox.security.entities.UserApi;
import br.com.workbox.security.services.RefreshTokenService.RotationStatus;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class JwtService extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(JwtService.class);

    private static final String TOKEN_TYPE_CLAIM = "typ";
    private static final String ACCESS_TOKEN_TYPE = "access";
    private static final String REFRESH_TOKEN_TYPE = "refresh";
    private static final String TOKEN_VERSION_CLAIM = "tv";
    private static final String JTI_CLAIM = "jti";
    private static final String FAMILY_CLAIM = "fam";
    private static final long ACCESS_TOKEN_VALIDITY_MS = 1_000 * 60 * 15L;
    private static final long REFRESH_TOKEN_VALIDITY_MS = 1_000 * 60 * 60 * 24L;

    private final SecretKey secretKey;
    private final UserApiService userApiService;
    private final RefreshTokenService refreshTokenService;

    @Autowired
    public JwtService(SecretKey secretKey, UserApiService userApiService, RefreshTokenService refreshTokenService) {
        this.secretKey = secretKey;
        this.userApiService = userApiService;
        this.refreshTokenService = refreshTokenService;
    }

    private String createToken(Map<String, Object> claims, String subject, long validityInMilliseconds) {
        final var now = new Date();
        final var validity = new Date(now.getTime() + validityInMilliseconds);
        return Jwts
                .builder()
                .setClaims(claims)
                .setSubject(subject)
                .setIssuedAt(now)
                .setExpiration(validity)
                .signWith(secretKey, SignatureAlgorithm.HS256)
                .compact();
    }

    public String generateToken(final UserApi user) {
        final var claims = new HashMap<String, Object>();
        claims.put(TOKEN_TYPE_CLAIM, ACCESS_TOKEN_TYPE);
        claims.put(TOKEN_VERSION_CLAIM, tokenVersionOf(user));
        claims.put("roles", user.getAuthorities().stream()
                .map(grantedAuthority -> {
                    String authority = grantedAuthority.getAuthority();
                    return authority.startsWith("ROLE_") ? authority : "ROLE_" + authority;
                })
                .toList());
        claims.put("scope", "read write");
        return createToken(claims, user.getUsername(), ACCESS_TOKEN_VALIDITY_MS);
    }

    /** Emite um refresh token abrindo uma nova família de rotação — uso exclusivo do login. */
    public String issueRefreshToken(final UserApi user) {
        return issueRefreshToken(user, UUID.randomUUID());
    }

    /**
     * Valida, roda e reemite o par de tokens a partir de um refresh token — uso exclusivo
     * de {@code /api/auth/refresh}. O {@code jti} apresentado é imediatamente revogado
     * (rotação: cada refresh token só serve uma vez); se um {@code jti} já revogado for
     * reapresentado, é reuso de token roubado — a família inteira é revogada e a
     * requisição rejeitada.
     */
    public TokenPair rotateRefreshToken(final String refreshToken) {
        final var claims = parseRefreshTokenClaims(refreshToken);
        if (claims.getExpiration().before(new Date())) {
            throw new InvalidRefreshTokenException("Token expired");
        }
        if (!REFRESH_TOKEN_TYPE.equals(claims.get(TOKEN_TYPE_CLAIM))) {
            throw new InvalidRefreshTokenException("Token is not a refresh token");
        }
        final UserApi user;
        try {
            user = (UserApi) userApiService.loadUserByUsername(claims.getSubject());
        } catch (UsernameNotFoundException e) {
            throw new InvalidRefreshTokenException("Invalid refresh token", e);
        }
        if (!Objects.equals(tokenVersionOf(user), claims.get(TOKEN_VERSION_CLAIM, Long.class))) {
            throw new InvalidRefreshTokenException("Token has been revoked");
        }

        final var jti = UUID.fromString(claims.get(JTI_CLAIM, String.class));
        final var result = refreshTokenService.consume(jti);

        if (result.status() == RotationStatus.NOT_FOUND) {
            throw new InvalidRefreshTokenException("Refresh token not recognized");
        }
        if (result.status() == RotationStatus.REUSED) {
            logger.warn("Refresh token reuse detected for user {} (family {}) — revoking whole family", user.getUsername(), result.familyId());
            throw new InvalidRefreshTokenException("Refresh token reuse detected");
        }

        return new TokenPair(generateToken(user), issueRefreshToken(user, result.familyId()));
    }

    private String issueRefreshToken(final UserApi user, final UUID familyId) {
        final var jti = UUID.randomUUID();
        final var expiresAt = LocalDateTime.now().plus(Duration.ofMillis(REFRESH_TOKEN_VALIDITY_MS));
        refreshTokenService.issue(user.getId(), familyId, jti, expiresAt);

        final var claims = new HashMap<String, Object>();
        claims.put(TOKEN_TYPE_CLAIM, REFRESH_TOKEN_TYPE);
        claims.put(TOKEN_VERSION_CLAIM, tokenVersionOf(user));
        claims.put(JTI_CLAIM, jti.toString());
        claims.put(FAMILY_CLAIM, familyId.toString());
        return createToken(claims, user.getUsername(), REFRESH_TOKEN_VALIDITY_MS);
    }

    public record TokenPair(String accessToken, String refreshToken) {
    }

    private long tokenVersionOf(final UserApi user) {
        return Objects.requireNonNullElse(user.getTokenVersion(), 0L);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull FilterChain filterChain) throws ServletException, IOException {
        final var authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            final var token = authHeader.substring(7);
            try {
                final var authenticationToken = getAuthentication(token);
                if (authenticationToken != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                    SecurityContextHolder.getContext().setAuthentication(authenticationToken);
                }
            } catch (InvalidTokenException e) {
                // Token malformado/expirado/assinatura inválida: segue sem autenticar em
                // vez de deixar a exception subir crua pelo filtro (rodaria antes do
                // DispatcherServlet, nenhum @ExceptionHandler pega isso — virava 500 até
                // em endpoints públicos como /api/auth/login). authorizeHttpRequests
                // decide o resto: público segue normal, protegido cai no
                // AuthenticationEntryPoint com 401.
                SecurityContextHolder.clearContext();
            }
        }
        filterChain.doFilter(request, response);
    }

    private io.jsonwebtoken.Claims parseRefreshTokenClaims(String refreshToken) {
        try {
            return Jwts.parserBuilder().setSigningKey(secretKey).build().parseClaimsJws(refreshToken).getBody();
        } catch (InvalidRefreshTokenException e) {
            throw e;
        } catch (Exception e) {
            throw new InvalidRefreshTokenException("Invalid refresh token", e);
        }
    }

    @SuppressWarnings("unchecked")
    private UsernamePasswordAuthenticationToken getAuthentication(String token) {
        try {
            final var claims = Jwts.parserBuilder().setSigningKey(secretKey).build().parseClaimsJws(token).getBody();
            if (!ACCESS_TOKEN_TYPE.equals(claims.get(TOKEN_TYPE_CLAIM))) {
                return null;
            }
            String username = claims.getSubject();
            if (username == null) {
                return null;
            }
            final var userDetail = (UserApi) userApiService.loadUserByUsername(username);
            if (!isAccountUsable(userDetail)) {
                return null;
            }
            if (!Objects.equals(tokenVersionOf(userDetail), claims.get(TOKEN_VERSION_CLAIM, Long.class))) {
                return null;
            }
            final var roles = (List<String>) claims.get("roles");
            List<GrantedAuthority> authorities = roles.stream()
                    .map(SimpleGrantedAuthority::new)
                    .collect(Collectors.toList());

            return new UsernamePasswordAuthenticationToken(username, null, authorities);
        } catch (JwtException e) {
            throw new InvalidTokenException("Invalid token or expired");
        } catch (UsernameNotFoundException e) {
            return null;
        }
    }

    private boolean isAccountUsable(UserDetails userDetails) {
        return userDetails.isEnabled()
                && userDetails.isAccountNonLocked()
                && userDetails.isAccountNonExpired()
                && userDetails.isCredentialsNonExpired();
    }
}
