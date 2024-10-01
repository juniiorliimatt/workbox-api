package br.com.api.security.dto;

import java.util.UUID;

public record UserApiUpdateDTO(UUID id, String username, String password) { }
