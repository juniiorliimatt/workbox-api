package br.com.workbox.security.controllers;

import br.com.workbox.security.dto.RoleDTO;
import br.com.workbox.security.services.RoleService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @author CLAUDE-CODE
 * @author Junior Lima - oojuniin@outlook.com
 * @since 29-08-2026
 */

@RestController
@RequestMapping("/api/v1/role")
public class RoleController {

    private final RoleService roleService;

    public RoleController(final RoleService roleService) {
        this.roleService = roleService;
    }

    /** Lista todas as roles ativas — Bearer USER ou ADMIN. */
    @GetMapping
    public ResponseEntity<List<RoleDTO>> findAll() {
        return ResponseEntity.ok(roleService.findAll());
    }

    /** Detalhe de uma role — Bearer USER ou ADMIN. */
    @GetMapping("/{id}")
    public ResponseEntity<RoleDTO> findById(@PathVariable final Long id) {
        return ResponseEntity.ok(roleService.findById(id));
    }

    /** Cria uma role nova — ADMIN-only. */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<RoleDTO> create(@RequestBody @Valid final RoleDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(roleService.create(dto));
    }

    /** Atualiza o authority de uma role — ADMIN-only. */
    @PutMapping("/{id}")
    public ResponseEntity<RoleDTO> update(@PathVariable final Long id, @RequestBody @Valid final RoleDTO dto) {
        return ResponseEntity.ok(roleService.update(id, dto));
    }

    /** Exclusão lógica de uma role — ADMIN-only. */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable final Long id) {
        roleService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
