package br.com.workbox.security.services;

import br.com.workbox.exceptions.ResourceNotFoundException;
import br.com.workbox.security.dto.RoleDTO;
import br.com.workbox.security.entities.Role;
import br.com.workbox.security.repositories.RoleRepository;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author CLAUDE-CODE
 * @author Junior Lima - oojuniin@outlook.com
 * @since 29-08-2026
 */

@Service
public class RoleService {

    private static final String ROLE_NOT_FOUND = "Role not found";

    private final RoleRepository roleRepository;

    public RoleService(final RoleRepository roleRepository) {
        this.roleRepository = roleRepository;
    }

    @Transactional(readOnly = true)
    public List<RoleDTO> findAll() {
        return roleRepository.findAll().stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public RoleDTO findById(final Long id) {
        return toDto(roleRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException(ROLE_NOT_FOUND)));
    }

    @Transactional
    public RoleDTO create(final RoleDTO dto) {
        final var saved = roleRepository.save(Role.builder().authority(dto.authority()).build());
        return toDto(saved);
    }

    @Transactional
    public RoleDTO update(final Long id, final RoleDTO dto) {
        final var role = roleRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException(ROLE_NOT_FOUND));
        role.setAuthority(dto.authority());
        return toDto(roleRepository.save(role));
    }

    /** Exclusão lógica — {@code @SQLRestriction} na entidade cuida do resto. */
    @Transactional
    public void delete(final Long id) {
        final var role = roleRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException(ROLE_NOT_FOUND));
        role.setDeletedAt(LocalDateTime.now());
        roleRepository.save(role);
    }

    private RoleDTO toDto(final Role role) {
        return new RoleDTO(role.getId(), role.getAuthority());
    }
}
