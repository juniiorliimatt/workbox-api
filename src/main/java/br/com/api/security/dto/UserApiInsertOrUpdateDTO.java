package br.com.api.security.dto;

import java.util.UUID;

public record UserApiInsertOrUpdateDTO(UUID id,
                                       String username,
                                       String password,
                                       boolean enabled) { }
