package br.com.api.security.controllers;

import br.com.api.core.UserApiFindAll;
import br.com.api.security.dto.UserApiDTO;
import br.com.api.security.dto.UserApiInsertOrUpdateDTO;
import br.com.api.security.services.UserApiService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/user")
public class UserApiController {

    private final UserApiService userApiService;

    @Autowired
    public UserApiController(UserApiService userApiService) {
        this.userApiService = userApiService;
    }

    @GetMapping
    @UserApiFindAll
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<Page<UserApiDTO>> findAll(Pageable pageable) {
        return ResponseEntity.ok(userApiService.findAll(pageable));
    }

    @GetMapping("/{id}")
    @UserApiFindAll
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<UserApiDTO> findById(@PathVariable UUID id) {
        return ResponseEntity.ok().body(userApiService.findById(id));
    }

    @PostMapping
    @UserApiFindAll
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<UserApiDTO> save(@RequestBody final UserApiInsertOrUpdateDTO userApiInsertOrUpdateDTO,final UriComponentsBuilder uriBuilder) {
        final var newUser = userApiService.save(userApiInsertOrUpdateDTO);
        URI uri = uriBuilder.path("/user/{id}").buildAndExpand(newUser.id()).toUri();
        return ResponseEntity.created(uri).body(newUser);
    }

    @PutMapping
    @UserApiFindAll
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<UserApiDTO> update(@RequestBody final UserApiInsertOrUpdateDTO userApiInsertOrUpdateDTO) {
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
