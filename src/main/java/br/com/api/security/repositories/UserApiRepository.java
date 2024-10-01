package br.com.api.security.repositories;

import br.com.api.security.entities.UserApi;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserApiRepository extends JpaRepository<UserApi, UUID> {

    Optional<UserApi> findByUsername(final String username);

}
