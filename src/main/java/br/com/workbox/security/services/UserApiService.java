package br.com.workbox.security.services;

import br.com.workbox.exceptions.ResourceNotFoundException;
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

import java.util.HashSet;
import java.util.List;
import java.util.UUID;

@Service
public class UserApiService implements UserDetailsService {

    private static final String USER_NOT_FOUND = "User not found";

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
        return list.map(user -> new UserApiDTO(user.getId(), user.getUsername(), user.isEnabled()));
    }

    @Transactional(readOnly = true)
    public List<UserApiDTO> findAll() {
        logger.info("find all users");
        final var list = userApiRepository.findAll();
        return list.stream().map(user -> new UserApiDTO(user.getId(), user.getUsername(), user.isEnabled())).toList();
    }

    @Transactional(readOnly = true)
    public UserApiDTO findById(final UUID id) {
        logger.info("find by id");
        final var user = userApiRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException(USER_NOT_FOUND));
        return new UserApiDTO(user.getId(),user.getUsername(), user.isEnabled());
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
        return new UserApiDTO(userSaved.getId(), userSaved.getUsername(), userSaved.isEnabled());
    }

    @Transactional
    public UserApiDTO update(final UserApiInsertOrUpdateDTO dto) {
        logger.info("update user");
        final var user = userApiRepository.findById(dto.id()).orElseThrow(() -> new ResourceNotFoundException(USER_NOT_FOUND));
        user.setUsername(dto.username());
        user.setPassword(passwordEncoder.encode(dto.password()));
        user.setIsEnabled(dto.isEnabled());
        final var updated = userApiRepository.save(user);
        return new UserApiDTO(updated.getId(),updated.getUsername(), updated.isEnabled());
    }

    @Transactional
    public void delete(final UUID id) {
        logger.info("delete user");
        final var user = userApiRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException(USER_NOT_FOUND));
        userApiRepository.deleteById(user.getId());
    }

    @Transactional(readOnly = true)
    public boolean validateCredentials(final String username, final String password) {
        logger.info("validate credentials");
        try {
            final var userDetails = loadUserByUsername(username);
            return isAccountUsable(userDetails) && passwordEncoder.matches(password, userDetails.getPassword());
        } catch (UsernameNotFoundException e) {
            // Não diferenciar "usuário inexistente" de "senha incorreta"/"conta desabilitada"
            // na resposta (evita enumeração de usuários via /api/auth/login).
            return false;
        }
    }

    private boolean isAccountUsable(final UserDetails userDetails) {
        return userDetails.isEnabled()
                && userDetails.isAccountNonLocked()
                && userDetails.isAccountNonExpired()
                && userDetails.isCredentialsNonExpired();
    }
}
