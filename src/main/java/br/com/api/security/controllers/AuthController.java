package br.com.api.security.controllers;

import br.com.api.core.UserApiFindAll;
import br.com.api.exceptions.InvalidRefreshTokenException;
import br.com.api.security.dto.UserApiDTO;
import br.com.api.security.dto.UserApiLoginDTO;
import br.com.api.security.services.JwtService;
import br.com.api.security.services.UserApiService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

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
    public ResponseEntity<Map<String, String>> login(@RequestBody UserApiLoginDTO userApiLoginDTO) {
        if (!userApiService.validateCredentials(userApiLoginDTO.username(), userApiLoginDTO.password())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Invalid username or password"));
        }
        String token = jwtService.generateToken(userApiLoginDTO.username());
        String refreshToken = jwtService.generateRefreshToken(userApiLoginDTO.username());
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

    @GetMapping("/user")
    @UserApiFindAll
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<List<UserApiDTO>> findAll() {
        return ResponseEntity.ok(userApiService.findAll());
    }

    @GetMapping("/user/{id}")
    @UserApiFindAll
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<UserApiDTO> findById(@PathVariable UUID id) {
        final var userDto = userApiService.findById(id);
        return ResponseEntity.ok().body(userDto);
    }

    @PostMapping("/user")
    @UserApiFindAll
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<UserApiDTO> save(@RequestBody final UserApiLoginDTO userApiLoginDTO,
                                           UriComponentsBuilder uriBuilder) {
        final var newUser = userApiService.save(userApiLoginDTO);
        URI uri = uriBuilder.path("/auth/user/{id}").buildAndExpand(newUser.id()).toUri();
        return ResponseEntity.created(uri).body(newUser);
    }
}
