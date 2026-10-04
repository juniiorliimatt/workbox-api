package br.com.workbox.security.services;

import br.com.workbox.security.entities.AppModule;
import br.com.workbox.security.entities.Role;
import br.com.workbox.security.entities.UserApi;
import br.com.workbox.security.repositories.ModuleRepository;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Regra de acesso a módulos: {@code ADMIN} acessa todo o catálogo; qualquer outro usuário
 * acessa só os módulos das roles que possui. {@code USER} (role inicial de todo cadastro)
 * não está ligada a módulo nenhum — o acesso só nasce quando um admin dá ao usuário uma
 * role de módulo.
 *
 * @author CLAUDE-CODE
 * @author Junior Lima - oojuniin@outlook.com
 * @since 03-10-2026
 */

@Service
public class ModuleAccessService {

    static final String ROLE_ADMIN = "ADMIN";

    private final ModuleRepository moduleRepository;

    public ModuleAccessService(final ModuleRepository moduleRepository) {
        this.moduleRepository = moduleRepository;
    }

    /** Códigos dos módulos que o usuário pode acessar, lidos do estado atual do banco. */
    @Transactional(readOnly = true)
    public Set<String> codigosDo(final UserApi user) {
        final var roles = user.getRoles();
        if (roles.stream().anyMatch(role -> ROLE_ADMIN.equals(role.getAuthority()))) {
            return moduleRepository.findAll().stream().map(AppModule::getCode).collect(Collectors.toSet());
        }
        return roles.stream()
                .filter(role -> role.getDeletedAt() == null)
                .map(Role::getModule)
                .filter(Objects::nonNull)
                .map(AppModule::getCode)
                .collect(Collectors.toSet());
    }
}
