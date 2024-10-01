package br.com.api.security.services;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Service
public class JwtService {

    private final SecretKey secretKey;

    @Autowired
    public JwtService(SecretKey secretKey) {
        this.secretKey = secretKey;
    }

    public String generateToken(final String username) {
        Map<String, Object> claims = new HashMap<>();
        return createToken(claims, username);
    }

    public String generateRefreshToken(String username) {
        Map<String, Object> claims = new HashMap<>();
        return createToken(claims, username, 1_000*24*60*60L);
    }

    private String createToken(Map<String, Object> claims, String subject, long validityInMilliseconds) {
        final var now = new Date();
        var validity = new Date(now.getTime() + validityInMilliseconds);

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(subject)
                .setIssuedAt(now)
                .setExpiration(validity)
                .signWith(secretKey, SignatureAlgorithm.HS256)
                .compact();
    }

    private String createToken(Map<String, Object> claims, String subject) {
        return createToken(claims, subject, 1_000*5*60L);
    }
}
