package br.com.api.security.controllers;

import br.com.api.core.UserApiFindAll;
import br.com.api.security.dto.UserApiDTO;
import br.com.api.security.services.UserApiService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "User API")
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
    public ResponseEntity<List<UserApiDTO>> findAll() {
        return ResponseEntity.ok(userApiService.findAll());
    }

}
