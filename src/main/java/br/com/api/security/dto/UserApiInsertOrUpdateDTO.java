package br.com.api.security.dto;

import br.com.api.security.entities.Role;

import java.util.Set;
import java.util.UUID;

public record UserApiInsertOrUpdateDTO(UUID id,
                                       String username,
                                       String password,
                                       boolean isEnabled,
                                       Set<Role> roles) { }
