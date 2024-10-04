package br.com.api.security.dto;

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
    private boolean enabled;

}
