package br.com.workbox.security.controllers;

import br.com.workbox.security.dto.LoginAuditDTO;
import br.com.workbox.security.dto.RoleRevisionDTO;
import br.com.workbox.security.dto.UserApiRevisionDTO;
import br.com.workbox.security.services.AuditService;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Consulta administrativa das trilhas de auditoria — {@code /api/v1/audit/**} é
 * ADMIN-only (ver {@code SecurityConfig}), diferente de {@code /api/v1/user/**} que
 * também libera USER pra self-service: histórico de login e de alteração de
 * usuário/role é dado sensível (IP, quem mudou o quê), não self-service.
 *
 * @author CLAUDE-CODE
 * @author Junior Lima - oojuniin@outlook.com
 * @since 30-08-2026
 */
@RestController
@RequestMapping("/api/v1/audit")
public class AuditController {

    private final AuditService auditService;

    public AuditController(final AuditService auditService) {
        this.auditService = auditService;
    }

    @GetMapping("/logins")
    public ResponseEntity<Page<LoginAuditDTO>> findLoginAudits(
            @RequestParam(required = false) final String email,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) final LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) final LocalDateTime to,
            final Pageable pageable) {
        return ResponseEntity.ok(auditService.findLoginAudits(email, from, to, pageable));
    }

    @GetMapping("/users/{id}/history")
    public ResponseEntity<List<UserApiRevisionDTO>> findUserHistory(@PathVariable final UUID id) {
        return ResponseEntity.ok(auditService.findUserHistory(id));
    }

    @GetMapping("/roles/{id}/history")
    public ResponseEntity<List<RoleRevisionDTO>> findRoleHistory(@PathVariable final Long id) {
        return ResponseEntity.ok(auditService.findRoleHistory(id));
    }
}
