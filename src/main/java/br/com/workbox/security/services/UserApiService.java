package br.com.workbox.security.services;

import br.com.workbox.exceptions.InvalidRequestException;
import br.com.workbox.exceptions.LoginInvalidException;
import br.com.workbox.exceptions.ResourceNotFoundException;
import br.com.workbox.exceptions.UserAlreadyExistsException;
import br.com.workbox.security.dto.ChangePasswordDTO;
import br.com.workbox.security.dto.RoleDTO;
import br.com.workbox.security.dto.UserApiDTO;
import br.com.workbox.security.dto.UserApiInsertOrUpdateDTO;
import br.com.workbox.security.dto.UserApiRegisterDTO;
import br.com.workbox.security.entities.Role;
import br.com.workbox.security.entities.UserApi;
import br.com.workbox.security.repositories.RoleRepository;
import br.com.workbox.security.repositories.UserApiRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.support.MessageSourceAccessor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * @author CLAUDE-CODE
 * @author Junior Lima - oojuniin@outlook.com
 * @since 29-08-2026
 */

@Service
public class UserApiService implements UserDetailsService {

    static final int MAX_FAILED_ATTEMPTS = 5;
    static final long LOCK_DURATION_MINUTES = 15;

    private static final Logger logger = LoggerFactory.getLogger(UserApiService.class);
    private final UserApiRepository userApiRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final MessageSourceAccessor messages;

    @Autowired
    public UserApiService(final UserApiRepository userApiRepository, final RoleRepository roleRepository,
                           final PasswordEncoder passwordEncoder, final MessageSourceAccessor messages) {
        this.userApiRepository = userApiRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.messages = messages;
    }

    /** Nome do método é o contrato de {@link UserDetailsService} — o identificador recebido é o email, não um username. */
    @Override
    public UserDetails loadUserByUsername(final String email) throws UsernameNotFoundException {
        logger.info("load by email");
        return userApiRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException(messages.getMessage("usuario.naoEncontrado")));
    }

    /**
     * Specification em vez de JPQL {@code (:search IS NULL OR ...)} — esse padrão quebra
     * contra Postgres real ("could not determine data type of parameter") quando o
     * parâmetro vem nulo, porque o driver não infere o tipo do bind só a partir de
     * "? IS NULL" (achado real em {@code AuditService}; H2/perfil de teste não reproduz).
     * Specification só adiciona o predicado quando {@code search} de fato veio preenchido.
     */
    @Transactional(readOnly = true)
    public Page<UserApiDTO> findAll(final String search, final Pageable pageable) {
        logger.info("find all users pageable");
        Specification<UserApi> spec = Specification.unrestricted();
        if (search != null && !search.isBlank()) {
            final var pattern = "%" + search.toLowerCase() + "%";
            spec = spec.and((root, query, cb) -> cb.or(
                    cb.like(cb.lower(root.get("socialName")), pattern),
                    cb.like(cb.lower(root.get("email")), pattern)));
        }
        final var list = userApiRepository.findAll(spec, pageable);
        return list.map(this::toDto);
    }

    /** Lista completa, sem paginação — usada pelo endpoint HATEOAS {@code /find-all}. */
    @Transactional(readOnly = true)
    public List<UserApiDTO> findAll() {
        logger.info("find all users");
        final var list = userApiRepository.findAll();
        return list.stream().map(this::toDto).toList();
    }

    /** Busca por id; lança {@link ResourceNotFoundException} (404) se não existir. */
    @Transactional(readOnly = true)
    public UserApiDTO findById(final UUID id) {
        logger.info("find by id");
        final var user = userApiRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException(messages.getMessage("usuario.naoEncontrado")));
        return toDto(user);
    }

    /** Perfil do usuário autenticado — {@code email} vem do {@code Authentication} da requisição, nunca de um id no payload. */
    @Transactional(readOnly = true)
    public UserApiDTO me(final String email) {
        final var user = (UserApi) loadUserByUsername(email);
        return toDto(user);
    }

    /** Criação de usuário pelo admin — diferente de {@link #cadastrar}, aceita roles do payload (ver {@link #resolveRoles}). */
    @Transactional
    public UserApiDTO save(final UserApiInsertOrUpdateDTO dto) {
        logger.info("save user");
        final var user = new UserApi(dto);
        user.setRoles(resolveRoles(dto.roles()));
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        final var userSaved = userApiRepository.save(user);
        return toDto(userSaved);
    }

    /** Roles vêm do payload só com o id preenchido — resolve pra entidade gerenciada, nunca confia em nome/authority enviados pelo client. */
    private Set<Role> resolveRoles(final Set<Role> requestedRoles) {
        logger.info("get role");
        final var roles = new HashSet<Role>();
        for (Role role : requestedRoles) {
            if (role.getId() == null) {
                throw new InvalidRequestException(messages.getMessage("role.idObrigatorio"));
            }
            final Role existingRole = roleRepository.findById(role.getId()).orElseThrow(() -> new ResourceNotFoundException(messages.getMessage("role.naoEncontrada")));
            roles.add(existingRole);
        }
        return roles;
    }

    /**
     * Auto-cadastro público: diferente de {@link #save}, nunca aceita roles do
     * chamador — sempre atribui USER, prevenindo escalonamento de privilégio via payload
     * (BOLA/mass assignment, OWASP API3:2023). Unicidade de email é checada aqui pra
     * devolver mensagem específica; o índice único parcial no banco continua como rede de
     * segurança contra corrida entre o check e o insert.
     */
    @Transactional
    public UserApiDTO cadastrar(final UserApiRegisterDTO dto) {
        if (userApiRepository.findByEmail(dto.email()).isPresent()) {
            throw new UserAlreadyExistsException(messages.getMessage("usuario.emailJaCadastrado"));
        }
        final var userRole = roleRepository.findByAuthority("USER")
                .orElseThrow(() -> new ResourceNotFoundException(messages.getMessage("role.naoEncontrada")));

        final var user = new UserApi();
        user.setSocialName(dto.socialName());
        user.setEmail(dto.email());
        user.setPassword(passwordEncoder.encode(dto.password()));
        user.setIsEnabled(true);
        user.setRoles(new HashSet<>(Set.of(userRole)));

        final var userSaved = userApiRepository.save(user);
        return toDto(userSaved);
    }

    /** Senha é opcional aqui — só re-hasheia se o client mandou uma nova (update de perfil sem trocar senha é o caso comum). */
    @Transactional
    public UserApiDTO update(final UserApiInsertOrUpdateDTO dto) {
        logger.info("update user");
        final var user = userApiRepository.findById(dto.id()).orElseThrow(() -> new ResourceNotFoundException(messages.getMessage("usuario.naoEncontrado")));
        user.setSocialName(dto.socialName());
        if (dto.password() != null && !dto.password().isBlank()) {
            user.setPassword(passwordEncoder.encode(dto.password()));
        }
        user.setEmail(dto.email());
        user.setIsEnabled(dto.isEnabled());
        if (dto.roles() != null) {
            user.setRoles(resolveRoles(dto.roles()));
        }
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
        final var user = userApiRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException(messages.getMessage("usuario.naoEncontrado")));
        user.setDeletedAt(LocalDateTime.now());
        userApiRepository.save(user);
    }

    /**
     * Login completo: aplica lockout automático por tentativas falhas e nunca revela,
     * via retorno, se a causa foi usuário inexistente, senha errada ou conta bloqueada
     * (o motivo detalhado é só pra log/auditoria, nunca pra resposta HTTP).
     */
    @Transactional
    public LoginAttemptResult tentarLogin(final String email, final String rawPassword) {
        final UserApi user;
        try {
            user = (UserApi) loadUserByUsername(email);
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
    public void logout(final String email) {
        final var user = (UserApi) loadUserByUsername(email);
        user.setTokenVersion(user.getTokenVersion() + 1);
        userApiRepository.save(user);
    }

    /** Troca de senha autenticada: exige a senha atual e revoga (bump de tokenVersion) todo token emitido antes. */
    @Transactional
    public void alterarSenha(final String email, final ChangePasswordDTO dto) {
        final var user = (UserApi) loadUserByUsername(email);
        if (!passwordEncoder.matches(dto.currentPassword(), user.getPassword())) {
            throw new LoginInvalidException(messages.getMessage("usuario.senhaAtualIncorreta"));
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
        final var avatarUrl = user.getAvatarFilename() == null ? null : "/api/v1/user/" + user.getId() + "/avatar";
        final var roles = user.getRoles().stream()
                .map(role -> new RoleDTO(role.getId(), role.getAuthority()))
                .collect(Collectors.toSet());
        return new UserApiDTO(user.getId(), user.getSocialName(), user.getEmail(), user.isEnabled(), avatarUrl, roles);
    }
}
