package br.com.workbox.security.controllers;

import br.com.workbox.core.UserApiFindAll;
import br.com.workbox.security.dto.UserApiDTO;
import br.com.workbox.security.dto.UserApiInsertOrUpdateDTO;
import br.com.workbox.security.services.UserApiService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.Link;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.UUID;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@RestController
@RequestMapping("/api/user")
public class UserApiController {

    private final UserApiService userApiService;

    @Autowired
    public UserApiController(UserApiService userApiService) {
        this.userApiService = userApiService;
    }

    @GetMapping("/pageable")
    @UserApiFindAll
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<Page<UserApiDTO>> findAll(Pageable pageable) {
        return ResponseEntity.ok(userApiService.findAll(pageable));
    }

    @GetMapping("/find-all")
    @UserApiFindAll
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<CollectionModel<UserApiDTO>> findAll() {
        final var list = userApiService.findAll();
        for (UserApiDTO userDto : list) {
            var userId = userDto.getId();
            Link selfLink = linkTo(UserApiController.class).slash(userId).withSelfRel();
            userDto.add(selfLink);
        }
        Link link = linkTo(UserApiController.class).withSelfRel();
        final var result = CollectionModel.of(list, link);
        return ResponseEntity.ok().body(result);
    }

    @GetMapping("/{id}")
    @UserApiFindAll
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<EntityModel<UserApiDTO>> findById(@PathVariable UUID id) {
        final var list = userApiService.findById(id);
        EntityModel<UserApiDTO> resource = EntityModel.of(list);
        resource.add(linkTo(methodOn(UserApiController.class).findById(id)).withSelfRel());
        resource.add(linkTo(methodOn(UserApiController.class).findAll()).withRel("all-users"));
        return ResponseEntity.ok().body(resource);
    }

    @PostMapping("/save")
    @UserApiFindAll
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<UserApiDTO> save(@RequestBody @Valid final UserApiInsertOrUpdateDTO userApiInsertOrUpdateDTO, final UriComponentsBuilder uriBuilder) {
        final var newUser = userApiService.save(userApiInsertOrUpdateDTO);
        URI uri = uriBuilder.path("/user/{id}").buildAndExpand(newUser.getId()).toUri();
        return ResponseEntity.created(uri).body(newUser);
    }

    @PutMapping("/update")
    @UserApiFindAll
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<UserApiDTO> update(@RequestBody @Valid final UserApiInsertOrUpdateDTO userApiInsertOrUpdateDTO) {
        final var updatedUser = userApiService.update(userApiInsertOrUpdateDTO);
        return ResponseEntity.ok().body(updatedUser);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<Void> delete(@PathVariable final UUID id) {
        userApiService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
