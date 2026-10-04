package br.com.workbox.security.services;

import br.com.workbox.security.dto.ModuleDTO;
import br.com.workbox.security.repositories.ModuleRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author CLAUDE-CODE
 * @author Junior Lima - oojuniin@outlook.com
 * @since 03-10-2026
 */

@Service
public class ModuleService {

    private final ModuleRepository moduleRepository;

    public ModuleService(final ModuleRepository moduleRepository) {
        this.moduleRepository = moduleRepository;
    }

    /** Catálogo de módulos disponíveis pra vínculo com roles. */
    @Transactional(readOnly = true)
    public List<ModuleDTO> findAll() {
        return moduleRepository.findAll().stream()
                .map(module -> new ModuleDTO(module.getId(), module.getCode(), module.getName()))
                .toList();
    }
}
