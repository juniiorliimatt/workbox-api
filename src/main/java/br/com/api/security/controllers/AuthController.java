package br.com.api.security.controllers;

import br.com.api.exceptions.InvalidRefreshTokenException;
import br.com.api.security.dto.UserApiLoginCredentialsDTO;
import br.com.api.security.services.JwtService;
import br.com.api.security.services.UserApiService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final JwtService jwtService;
    private final UserApiService userApiService;

    @Autowired
    public AuthController(JwtService jwtService, UserApiService userApiService) {
        this.jwtService = jwtService;
        this.userApiService = userApiService;
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, String>> login(@RequestBody UserApiLoginCredentialsDTO userApiLoginCredentialsDTO) {
        if (!userApiService.validateCredentials(userApiLoginCredentialsDTO.username(), userApiLoginCredentialsDTO.password())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Invalid username or password"));
        }
        String token = jwtService.generateToken(userApiLoginCredentialsDTO.username());
        String refreshToken = jwtService.generateRefreshToken(userApiLoginCredentialsDTO.username());
        Map<String, String> tokens = new HashMap<>();
        tokens.put("access_token", token);
        tokens.put("refresh_token", refreshToken);
        return ResponseEntity.ok(tokens);
    }

    @PostMapping("/refresh")
    public ResponseEntity<Map<String, String>> refresh(@RequestParam String refreshToken) {
        try {
            String username = jwtService.validateRefreshToken(refreshToken);
            String newAccessToken = jwtService.generateToken(username);
            Map<String, String> tokens = new HashMap<>();
            tokens.put("access_token", newAccessToken);
            return ResponseEntity.ok(tokens);
        } catch (InvalidRefreshTokenException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Invalid or expired refresh token"));
        }
    }
}
