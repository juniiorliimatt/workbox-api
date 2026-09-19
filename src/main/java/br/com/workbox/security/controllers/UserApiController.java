package br.com.workbox.security.controllers;

import br.com.workbox.core.UserApiFindAll;
import br.com.workbox.security.dto.UserApiDTO;
import br.com.workbox.security.dto.UserApiInsertOrUpdateDTO;
import br.com.workbox.security.services.AvatarService;
import br.com.workbox.security.services.UserApiService;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.Link;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.UUID;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

/**
 * @author CLAUDE-CODE
 * @author Junior Lima - oojuniin@outlook.com
 * @since 29-08-2026
 */

@RestController
@RequestMapping("/api/v1/user")
public class UserApiController {

    private final UserApiService userApiService;
    private final AvatarService avatarService;

    @Autowired
    public UserApiController(final UserApiService userApiService, final AvatarService avatarService) {
        this.userApiService = userApiService;
        this.avatarService = avatarService;
    }

    /**
     * {@code search} filtra por substring (case-insensitive) em socialName OU email.
     * Nome do método diferente de {@link #findAll()} de propósito: springdoc-openapi
     * confunde metadado de parâmetro (`@Parameter`/`@UserApiFindAll`) entre métodos
     * sobrecarregados com o mesmo nome no mesmo controller — o parâmetro real
     * ({@code search}) aparecia substituído pelo fantasma de {@link #findAll()} no
     * contrato gerado até esse método ganhar um nome próprio.
     */
    @GetMapping("/pageable")
    @Parameter(name = "search", description = "Filtro por substring (case-insensitive) em socialName ou email", required = false)
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<Page<UserApiDTO>> findAllPageable(@RequestParam(required = false) final String search, final Pageable pageable) {
        return ResponseEntity.ok(userApiService.findAll(search, pageable));
    }

    /** Lista completa (HATEOAS), sem paginação — cada item ganha o próprio self-link. */
    @GetMapping("/find-all")
    @UserApiFindAll
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<CollectionModel<UserApiDTO>> findAll() {
        final var list = userApiService.findAll();
        for (UserApiDTO userDto : list) {
            final var userId = userDto.getId();
            final Link selfLink = linkTo(UserApiController.class).slash(userId).withSelfRel();
            userDto.add(selfLink);
        }
        final Link link = linkTo(UserApiController.class).withSelfRel();
        final var result = CollectionModel.of(list, link);
        return ResponseEntity.ok().body(result);
    }

    /** Detalhe de um usuário (HATEOAS), com link pra si mesmo e pra listagem completa. */
    @GetMapping("/{id}")
    @UserApiFindAll
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<EntityModel<UserApiDTO>> findById(@PathVariable final UUID id) {
        final var list = userApiService.findById(id);
        final EntityModel<UserApiDTO> resource = EntityModel.of(list);
        resource.add(linkTo(methodOn(UserApiController.class).findById(id)).withSelfRel());
        resource.add(linkTo(methodOn(UserApiController.class).findAll()).withRel("all-users"));
        return ResponseEntity.ok().body(resource);
    }

    /** Bytes do avatar de qualquer usuário (não só o próprio) — ADMIN/USER, ver regra de autorização em {@code API_USER}. */
    @GetMapping("/{id}/avatar")
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<byte[]> getAvatar(@PathVariable final UUID id) {
        final var content = avatarService.carregar(id);
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(content.contentType())).body(content.bytes());
    }

    /** Criação de usuário pelo admin (ADMIN-only) — auto-cadastro público é {@code POST /api/v1/auth/register}. */
    @PostMapping("/save")
    @UserApiFindAll
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<UserApiDTO> save(@RequestBody @Valid final UserApiInsertOrUpdateDTO userApiInsertOrUpdateDTO, final UriComponentsBuilder uriBuilder) {
        final var newUser = userApiService.save(userApiInsertOrUpdateDTO);
        final URI uri = uriBuilder.path("/api/v1/user/{id}").buildAndExpand(newUser.getId()).toUri();
        return ResponseEntity.created(uri).body(newUser);
    }

    /** Atualização pelo admin (ADMIN-only) — roles omitidas no payload preservam as atuais, ver {@link UserApiService#update}. */
    @PutMapping("/update")
    @UserApiFindAll
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<UserApiDTO> update(@RequestBody @Valid final UserApiInsertOrUpdateDTO userApiInsertOrUpdateDTO) {
        final var updatedUser = userApiService.update(userApiInsertOrUpdateDTO);
        return ResponseEntity.ok().body(updatedUser);
    }

    /** Exclusão lógica pelo admin (ADMIN-only) — ver {@link UserApiService#delete}. */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<Void> delete(@PathVariable final UUID id) {
        userApiService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
