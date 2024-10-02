package br.com.api.security.dto;

import java.util.UUID;

public record UserApiDTO(UUID id, String username, boolean enabled) { }
