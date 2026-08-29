package br.com.workbox.security.dto;

import br.com.workbox.security.entities.Role;

import java.util.Set;
import java.util.UUID;

/**
 * @author CLAUDE-CODE
 * @author Junior Lima - oojuniin@outlook.com
 * @since 29-08-2026
 */

public record UserApiInsertOrUpdateDTO(UUID id,
                                       String username,
                                       String password,
                                       String email,
                                       boolean isEnabled,
                                       Set<Role> roles) { }
