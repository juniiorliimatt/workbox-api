package br.com.workbox.security.services;

import br.com.workbox.exceptions.InvalidRequestException;
import br.com.workbox.exceptions.ResourceNotFoundException;
import br.com.workbox.security.dto.ModuleDTO;
import br.com.workbox.security.dto.RoleDTO;
import br.com.workbox.security.dto.RoleModuleDTO;
import br.com.workbox.security.entities.Role;
import br.com.workbox.security.repositories.ModuleRepository;
import br.com.workbox.security.repositories.RoleRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import org.springframework.context.support.MessageSourceAccessor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author CLAUDE-CODE
 * @author Junior Lima - oojuniin@outlook.com
 * @since 29-08-2026
 */

@Service
public class RoleService {

    private static final Set<String> ROLES_DE_SISTEMA = Set.of("ADMIN", "USER");

    private final RoleRepository roleRepository;
    private final ModuleRepository moduleRepository;
    private final MessageSourceAccessor messages;

    public RoleService(final RoleRepository roleRepository, final ModuleRepository moduleRepository, final MessageSourceAccessor messages) {
        this.roleRepository = roleRepository;
        this.moduleRepository = moduleRepository;
        this.messages = messages;
    }

    /** Lista todas as roles ativas (exclusão lógica já filtrada pela entidade). */
    @Transactional(readOnly = true)
    public List<RoleDTO> findAll() {
        return roleRepository.findAll().stream().map(this::toDto).toList();
    }

    /** Busca por id; lança {@link ResourceNotFoundException} (404) se não existir. */
    @Transactional(readOnly = true)
    public RoleDTO findById(final Long id) {
        return toDto(roleRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException(messages.getMessage("role.naoEncontrada"))));
    }

    /** Cria uma role nova a partir do {@code authority} informado. */
    @Transactional
    public RoleDTO create(final RoleDTO dto) {
        final var saved = roleRepository.save(Role.builder().authority(dto.authority()).build());
        return toDto(saved);
    }

    /** Atualiza o {@code authority} de uma role existente. */
    @Transactional
    public RoleDTO update(final Long id, final RoleDTO dto) {
        final var role = roleRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException(messages.getMessage("role.naoEncontrada")));
        role.setAuthority(dto.authority());
        return toDto(roleRepository.save(role));
    }

    /** Exclusão lógica — {@code @SQLRestriction} na entidade cuida do resto. */
    @Transactional
    public void delete(final Long id) {
        final var role = roleRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException(messages.getMessage("role.naoEncontrada")));
        role.setDeletedAt(LocalDateTime.now());
        roleRepository.save(role);
    }

    /**
     * Vincula a role a um módulo (ou desvincula, com {@code moduleId} nulo). {@code ADMIN}
     * (acessa tudo) e {@code USER} (role inicial, sem acesso a módulo) ficam fora do vínculo.
     * Vale na hora: o acesso é recalculado a cada introspecção, sem esperar o token expirar.
     */
    @Transactional
    public RoleDTO vincularModulo(final Long id, final RoleModuleDTO dto) {
        final var role = roleRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException(messages.getMessage("role.naoEncontrada")));
        if (ROLES_DE_SISTEMA.contains(role.getAuthority())) {
            throw new InvalidRequestException(messages.getMessage("role.sistemaNaoVinculavel"));
        }
        final var module = dto.moduleId() == null ? null
                : moduleRepository.findById(dto.moduleId()).orElseThrow(() -> new ResourceNotFoundException(messages.getMessage("modulo.naoEncontrado")));
        role.setModule(module);
        return toDto(roleRepository.save(role));
    }

    private RoleDTO toDto(final Role role) {
        final var module = role.getModule();
        return new RoleDTO(role.getId(), role.getAuthority(),
                module == null ? null : new ModuleDTO(module.getId(), module.getCode(), module.getName()));
    }
}
