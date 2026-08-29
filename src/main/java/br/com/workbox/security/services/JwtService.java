package br.com.workbox.security.services;

import br.com.workbox.exceptions.InvalidRefreshTokenException;
import br.com.workbox.exceptions.InvalidTokenException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.OctetSequenceKey;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class JwtService extends OncePerRequestFilter {

    private final SecretKey secretKey;
    private final UserApiService userApiService;

    @Autowired
    public JwtService(SecretKey secretKey, UserApiService userApiService) {
        this.secretKey = secretKey;
        this.userApiService = userApiService;
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

    private static final String TOKEN_TYPE_CLAIM = "typ";
    private static final String ACCESS_TOKEN_TYPE = "access";
    private static final String REFRESH_TOKEN_TYPE = "refresh";

    public String generateToken(final UserDetails userDetails) {
        final var claims = new HashMap<String, Object>();
        claims.put(TOKEN_TYPE_CLAIM, ACCESS_TOKEN_TYPE);
        claims.put("roles", userDetails.getAuthorities().stream()
                .map(grantedAuthority -> {
                    String authority = grantedAuthority.getAuthority();
                    return authority.startsWith("ROLE_") ? authority : "ROLE_" + authority;
                })
                .toList());
        claims.put("scope", "read write");
        return createToken(claims, userDetails.getUsername(), 1_000 * 60 * 15L);
    }

    public String generateRefreshToken(String username) {
        final var claims = new HashMap<String, Object>();
        claims.put(TOKEN_TYPE_CLAIM, REFRESH_TOKEN_TYPE);
        return createToken(claims, username, 1_000 * 60 * 60 * 24L);
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

    public String validateRefreshToken(String refreshToken) {
        final var claims = parseRefreshTokenClaims(refreshToken);
        if (claims.getExpiration().before(new Date())) {
            throw new InvalidRefreshTokenException("Token expired");
        }
        if (!REFRESH_TOKEN_TYPE.equals(claims.get(TOKEN_TYPE_CLAIM))) {
            throw new InvalidRefreshTokenException("Token is not a refresh token");
        }
        return claims.getSubject();
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

    @Bean
    public JwtEncoder jwtEncoder() {
        final var jwk = new OctetSequenceKey.Builder(secretKey).algorithm(JWSAlgorithm.HS256).build();
        final var jwkSet = new JWKSet(jwk);
        return new NimbusJwtEncoder((jwkSelector, context) -> jwkSelector.select(jwkSet));
    }

    @Bean
    public JwtDecoder jwtDecoder() {
        return NimbusJwtDecoder.withSecretKey(secretKey).build();
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
            final var userDetail = userApiService.loadUserByUsername(username);
            if (!isAccountUsable(userDetail)) {
                return null;
            }
            final var roles = (List<String>) claims.get("roles");
            List<GrantedAuthority> authorities = roles.stream()
                    .map(SimpleGrantedAuthority::new)
                    .collect(Collectors.toList());

            return new UsernamePasswordAuthenticationToken(username, null, authorities);
        } catch (JwtException e) {
            throw new InvalidTokenException("Invalid token or expired");
        }
    }

    private boolean isAccountUsable(UserDetails userDetails) {
        return userDetails.isEnabled()
                && userDetails.isAccountNonLocked()
                && userDetails.isAccountNonExpired()
                && userDetails.isCredentialsNonExpired();
    }
}
