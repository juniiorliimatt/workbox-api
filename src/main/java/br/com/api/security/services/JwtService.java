package br.com.api.security.services;

import br.com.api.exceptions.InvalidRefreshTokenException;
import br.com.api.exceptions.InvalidTokenException;
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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
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
import java.util.Map;

@Service
public class JwtService extends OncePerRequestFilter {

    private final SecretKey secretKey;
    private final UserDetailsService userDetailsService;

    @Autowired
    public JwtService(SecretKey secretKey, UserDetailsService userDetailsService) {
        this.secretKey = secretKey;
        this.userDetailsService = userDetailsService;
    }

    private String createToken(Map<String, Object> claims, String subject, long validityInMilliseconds) {
        final var now = new Date();
        final var validity = new Date(now.getTime() + validityInMilliseconds);
        return Jwts.builder().setClaims(claims).setSubject(subject).setIssuedAt(now).setExpiration(validity).signWith(secretKey, SignatureAlgorithm.HS256).compact();
    }

    public String generateToken(final String username) {
        final var claims = new HashMap<String, Object>();
        return createToken(claims, username, 1_000 * 60 * 15L);
    }

    public String generateRefreshToken(String username) {
        final var claims = new HashMap<String, Object>();
        return createToken(claims, username, 1_000 * 60 * 60 * 24L);
    }

    private String validateToken(String token) {
        try {
            final var claims = Jwts.parserBuilder().setSigningKey(secretKey).build().parseClaimsJws(token).getBody();
            return claims.getSubject();
        } catch (JwtException e) {
            throw new InvalidTokenException("Invalid token or expired");
        }
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        final var authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            final var token = authHeader.substring(7);
            final var username = validateToken(token);
            if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails userDetails = userDetailsService.loadUserByUsername(username);
                UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                SecurityContextHolder.getContext().setAuthentication(authenticationToken);
            }
        }
        filterChain.doFilter(request, response);
    }

    public String validateRefreshToken(String refreshToken) {
        try {
            final var claims = Jwts.parserBuilder().setSigningKey(secretKey).build().parseClaimsJws(refreshToken).getBody();
            if (claims.getExpiration().before(new Date())) {
                throw new InvalidRefreshTokenException("Token expired");
            }
            return claims.getSubject();
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
}
