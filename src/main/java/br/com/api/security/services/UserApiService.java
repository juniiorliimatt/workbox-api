package br.com.api.security.services;

import br.com.api.exceptions.DatabaseException;
import br.com.api.exceptions.ResourceNotFoundException;
import br.com.api.security.dto.UserApiDTO;
import br.com.api.security.dto.UserApiInsertOrUpdateDTO;
import br.com.api.security.entities.UserApi;
import br.com.api.security.repositories.UserApiRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class UserApiService implements UserDetailsService {

    private static final String USER_NOT_FOUND = "User not found";

    private static final Logger logger = LoggerFactory.getLogger(UserApiService.class);
    private final UserApiRepository userApiRepository;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public UserApiService(final UserApiRepository userApiRepository, final PasswordEncoder passwordEncoder) {
        this.userApiRepository = userApiRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public UserDetails loadUserByUsername(final String username) throws UsernameNotFoundException {
        logger.info("load by username");
        return userApiRepository.findByUsername(username).orElseThrow(() -> new ResourceNotFoundException(USER_NOT_FOUND));
    }

    @Transactional(readOnly = true)
    public Page<UserApiDTO> findAll(Pageable pageable) {
        logger.info("find all users");
        final var list = userApiRepository.findAll(pageable);
        return list.map(user -> new UserApiDTO(user.getId(), user.getUsername(), user.getEnabled()));
    }

    @Transactional(readOnly = true)
    public UserApiDTO findById(final UUID id) {
        final var user = userApiRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException(USER_NOT_FOUND));
        return new UserApiDTO(user.getId(),user.getUsername(), user.getEnabled());
    }

    @Transactional
    public UserApiDTO save(final UserApiInsertOrUpdateDTO dto) {
        final var user = new UserApi(dto);
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        final var userSaved = userApiRepository.save(user);
        return new UserApiDTO(userSaved.getId(), userSaved.getUsername(), userSaved.getEnabled());
    }

    @Transactional
    public UserApiDTO update(final UserApiInsertOrUpdateDTO dto) {
        final var user = userApiRepository.findById(dto.id()).orElseThrow(() -> new ResourceNotFoundException(USER_NOT_FOUND));
        user.setUsername(dto.username());
        user.setPassword(passwordEncoder.encode(dto.password()));
        final var updated = userApiRepository.save(user);
        return new UserApiDTO(updated.getId(),updated.getUsername(), user.getEnabled());
    }

    @Transactional
    public void delete(final UUID id) {
        try {
            final var user = userApiRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException(USER_NOT_FOUND));
            userApiRepository.deleteById(user.getId());
        } catch (IllegalArgumentException e) {
            throw new ResourceNotFoundException("Id is null");
        } catch (DataIntegrityViolationException e) {
            throw new DatabaseException("Integrity violation");
        }

    }

    @Transactional
    public boolean validateCredentials(final String username, final String password) {
        final var userApiOptional = loadUserByUsername(username);
        return passwordEncoder.matches(password, userApiOptional.getPassword());
    }
}
