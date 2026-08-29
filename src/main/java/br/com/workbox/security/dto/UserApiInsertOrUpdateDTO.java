package br.com.workbox.security.dto;

import br.com.workbox.security.entities.Role;

import java.util.Set;
import java.util.UUID;

public record UserApiInsertOrUpdateDTO(UUID id,
                                       String username,
                                       String password,
                                       boolean isEnabled,
                                       Set<Role> roles) { }
