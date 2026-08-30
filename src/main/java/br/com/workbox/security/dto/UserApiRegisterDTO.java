package br.com.workbox.security.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * @author CLAUDE-CODE
 * @author Junior Lima - oojuniin@outlook.com
 * @since 30-08-2026
 */

public record UserApiRegisterDTO(@NotBlank(message = "Social name is mandatory")
                                  @Size(min = 2, max = 120)
                                  String socialName,

                                  @NotBlank(message = "Email is mandatory")
                                  @Email(message = "Email must be valid")
                                  String email,

                                  @NotBlank(message = "Password is mandatory")
                                  @Size(min = 8, max = 100)
                                  String password) { }
