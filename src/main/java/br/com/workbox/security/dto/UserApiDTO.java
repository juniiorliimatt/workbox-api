package br.com.workbox.security.dto;

import lombok.*;
import org.springframework.hateoas.RepresentationModel;

import java.util.UUID;

/**
 * @author CLAUDE-CODE
 * @author Junior Lima - oojuniin@outlook.com
 * @since 29-08-2026
 */

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class UserApiDTO extends RepresentationModel<UserApiDTO> {

    private UUID id;
    private String socialName;
    private String email;
    private boolean enabled;
    private String avatarUrl;

}
