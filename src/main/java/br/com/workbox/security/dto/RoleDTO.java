package br.com.workbox.security.dto;

import jakarta.validation.constraints.NotBlank;

public record RoleDTO(Long id, @NotBlank(message = "Field authority is required") String authority) { }
