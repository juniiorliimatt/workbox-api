package br.com.api.security.services;

import br.com.api.security.dto.UserApiDTO;
import br.com.api.security.dto.UserApiLoginDTO;
import br.com.api.security.dto.UserApiUpdateDTO;
import br.com.api.security.entities.UserApi;
import br.com.api.security.repositories.UserApiRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class UserApiService implements UserDetailsService {

    private final UserApiRepository userApiRepository;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public UserApiService(final UserApiRepository userApiRepository, final PasswordEncoder passwordEncoder) {
        this.userApiRepository = userApiRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public UserDetails loadUserByUsername(final String username) throws UsernameNotFoundException {
        return userApiRepository.findByUsername(username).orElseThrow(() -> new UsernameNotFoundException("User " + username + " not found"));
    }

    public UserApiDTO save(final UserApiLoginDTO dto) {
        final var user = new UserApi(dto);
        final var userSaved = userApiRepository.save(user);
        return new UserApiDTO(userSaved.getId(), userSaved.getUsername());
    }

    public List<UserApiDTO> findAll() {
        return userApiRepository.findAll().stream().map(user -> new UserApiDTO(user.getId(),user.getUsername())).toList();
    }

    public UserApiDTO findById(final UUID id) {
        final var user = userApiRepository.findById(id).orElseThrow(() -> new UsernameNotFoundException("User not found"));
        return new UserApiDTO(user.getId(),user.getUsername());
    }

    public UserApiDTO update(final UserApiUpdateDTO dto) {
        final var user = userApiRepository.findById(dto.id()).orElseThrow(() -> new UsernameNotFoundException("User not found"));
        user.setUsername(dto.username());
        user.setPassword(dto.password());
        final var updated = userApiRepository.save(user);
        return new UserApiDTO(updated.getId(),updated.getUsername());
    }

    public void delete(final UUID id) {
        userApiRepository.deleteById(id);
    }

    public boolean validateCredentials(final String username, final String password) {
        Optional<UserApi> userApiOptional = userApiRepository.findByUsername(username);
        return userApiOptional.isPresent() && passwordEncoder.matches(password, userApiOptional.get().getPassword());
    }
}
