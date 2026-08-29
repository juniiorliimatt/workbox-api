package br.com.workbox.security.dto;

import lombok.*;
import org.springframework.hateoas.RepresentationModel;

import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class UserApiDTO extends RepresentationModel<UserApiDTO> {

    private UUID id;
    private String username;
    private String email;
    private boolean enabled;

}
