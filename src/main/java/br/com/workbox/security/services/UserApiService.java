package br.com.workbox.security.services;

import br.com.workbox.exceptions.LoginInvalidException;
import br.com.workbox.exceptions.ResourceNotFoundException;
import br.com.workbox.security.dto.ChangePasswordDTO;
import br.com.workbox.security.dto.UserApiDTO;
import br.com.workbox.security.dto.UserApiInsertOrUpdateDTO;
import br.com.workbox.security.entities.Role;
import br.com.workbox.security.entities.UserApi;
import br.com.workbox.security.repositories.RoleRepository;
import br.com.workbox.security.repositories.UserApiRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

/**
 * @author CLAUDE-CODE
 * @author Junior Lima - oojuniin@outlook.com
 * @since 29-08-2026
 */

@Service
public class UserApiService implements UserDetailsService {

    private static final String USER_NOT_FOUND = "User not found";
    static final int MAX_FAILED_ATTEMPTS = 5;
    static final long LOCK_DURATION_MINUTES = 15;

    private static final Logger logger = LoggerFactory.getLogger(UserApiService.class);
    private final UserApiRepository userApiRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public UserApiService(final UserApiRepository userApiRepository, final RoleRepository roleRepository, final PasswordEncoder passwordEncoder) {
        this.userApiRepository = userApiRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public UserDetails loadUserByUsername(final String username) throws UsernameNotFoundException {
        logger.info("load by username");
        return userApiRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException(USER_NOT_FOUND));
    }

    @Transactional(readOnly = true)
    public Page<UserApiDTO> findAll(Pageable pageable) {
        logger.info("find all users pageable");
        final var list = userApiRepository.findAll(pageable);
        return list.map(this::toDto);
    }

    @Transactional(readOnly = true)
    public List<UserApiDTO> findAll() {
        logger.info("find all users");
        final var list = userApiRepository.findAll();
        return list.stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public UserApiDTO findById(final UUID id) {
        logger.info("find by id");
        final var user = userApiRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException(USER_NOT_FOUND));
        return toDto(user);
    }

    @Transactional(readOnly = true)
    public UserApiDTO me(final String username) {
        final var user = (UserApi) loadUserByUsername(username);
        return toDto(user);
    }

    @Transactional
    public UserApiDTO save(final UserApiInsertOrUpdateDTO dto) {
        logger.info("get role");
        final var roles = new HashSet<Role>();
        for (Role role : dto.roles()) {
            Role existingRole = roleRepository.findById(role.getId()).orElseThrow(() -> new ResourceNotFoundException("Role not found"));
            roles.add(existingRole);
        }
        logger.info("save user");
        final var user = new UserApi(dto);
        user.setRoles(roles);
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        final var userSaved = userApiRepository.save(user);
        return toDto(userSaved);
    }

    @Transactional
    public UserApiDTO update(final UserApiInsertOrUpdateDTO dto) {
        logger.info("update user");
        final var user = userApiRepository.findById(dto.id()).orElseThrow(() -> new ResourceNotFoundException(USER_NOT_FOUND));
        user.setUsername(dto.username());
        user.setPassword(passwordEncoder.encode(dto.password()));
        user.setEmail(dto.email());
        user.setIsEnabled(dto.isEnabled());
        final var updated = userApiRepository.save(user);
        return toDto(updated);
    }

    /**
     * Exclusão lógica — marca deletedAt em vez de apagar a linha. {@code @SQLRestriction}
     * na entidade já filtra usuários deletados de toda consulta normal, então isso basta
     * pra "sumir" o usuário sem perder o histórico nem invalidar FKs (login_audit,
     * password_reset_tokens, user_roles).
     */
    @Transactional
    public void delete(final UUID id) {
        logger.info("delete user (soft)");
        final var user = userApiRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException(USER_NOT_FOUND));
        user.setDeletedAt(LocalDateTime.now());
        userApiRepository.save(user);
    }

    /**
     * Login completo: aplica lockout automático por tentativas falhas e nunca revela,
     * via retorno, se a causa foi usuário inexistente, senha errada ou conta bloqueada
     * (o motivo detalhado é só pra log/auditoria, nunca pra resposta HTTP).
     */
    @Transactional
    public LoginAttemptResult attemptLogin(final String username, final String rawPassword) {
        final UserApi user;
        try {
            user = (UserApi) loadUserByUsername(username);
        } catch (UsernameNotFoundException e) {
            return LoginAttemptResult.failure("unknown_user");
        }

        if (!isAccountUsable(user)) {
            return LoginAttemptResult.failure("account_not_usable");
        }

        if (!passwordEncoder.matches(rawPassword, user.getPassword())) {
            registerFailedAttempt(user);
            return LoginAttemptResult.failure("invalid_password");
        }

        if (user.getFailedLoginAttempts() != 0) {
            user.setFailedLoginAttempts(0);
            userApiRepository.save(user);
        }
        return LoginAttemptResult.success(user);
    }

    private void registerFailedAttempt(final UserApi user) {
        final int attempts = user.getFailedLoginAttempts() + 1;
        user.setFailedLoginAttempts(attempts);
        if (attempts >= MAX_FAILED_ATTEMPTS) {
            user.setLockedUntil(LocalDateTime.now().plusMinutes(LOCK_DURATION_MINUTES));
            user.setFailedLoginAttempts(0);
        }
        userApiRepository.save(user);
    }

    /** Bump de tokenVersion — invalida todo access/refresh token emitido antes disso. */
    @Transactional
    public void logout(final String username) {
        final var user = (UserApi) loadUserByUsername(username);
        user.setTokenVersion(user.getTokenVersion() + 1);
        userApiRepository.save(user);
    }

    @Transactional
    public void changePassword(final String username, final ChangePasswordDTO dto) {
        final var user = (UserApi) loadUserByUsername(username);
        if (!passwordEncoder.matches(dto.currentPassword(), user.getPassword())) {
            throw new LoginInvalidException("Current password is incorrect");
        }
        user.setPassword(passwordEncoder.encode(dto.newPassword()));
        user.setTokenVersion(user.getTokenVersion() + 1);
        userApiRepository.save(user);
    }

    private boolean isAccountUsable(final UserDetails userDetails) {
        return userDetails.isEnabled()
                && userDetails.isAccountNonLocked()
                && userDetails.isAccountNonExpired()
                && userDetails.isCredentialsNonExpired();
    }

    private UserApiDTO toDto(final UserApi user) {
        return new UserApiDTO(user.getId(), user.getUsername(), user.getEmail(), user.isEnabled());
    }
}
