package br.com.workbox.security.controllers;

import br.com.workbox.security.dto.ModuleDTO;
import br.com.workbox.security.services.ModuleService;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author CLAUDE-CODE
 * @author Junior Lima - oojuniin@outlook.com
 * @since 03-10-2026
 */

@RestController
@RequestMapping("/api/v1/module")
public class ModuleController {

    private final ModuleService moduleService;

    public ModuleController(final ModuleService moduleService) {
        this.moduleService = moduleService;
    }

    /** Lista os módulos do catálogo — ADMIN-only. */
    @GetMapping
    public ResponseEntity<List<ModuleDTO>> findAll() {
        return ResponseEntity.ok(moduleService.findAll());
    }
}
