package br.com.api.security.dto;

public record UserApiLoginDTO(String username, String password, boolean enabled) { }
