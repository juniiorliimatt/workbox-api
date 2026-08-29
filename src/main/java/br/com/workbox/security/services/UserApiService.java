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
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

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
        var user = userApiRepository.findByUsername(username).orElseThrow(() -> new ResourceNotFoundException(USER_NOT_FOUND));
        List<GrantedAuthority> authorities = user.getRoles().stream()
                .map(role -> new SimpleGrantedAuthority(role.getAuthority()))
                .collect(Collectors.toList());

        return new org.springframework.security.core.userdetails.User(
                user.getUsername(),
                user.getPassword(),
                authorities
        );
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

    @Transactional
    public boolean validateCredentials(final String username, final String password) {
        logger.info("validate credentials");
        final var userApiOptional = loadUserByUsername(username);
        return passwordEncoder.matches(password, userApiOptional.getPassword());
    }
}
