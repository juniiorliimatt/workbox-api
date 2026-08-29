package br.com.workbox.security.repositories;

import br.com.workbox.security.entities.RefreshToken;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * @author CLAUDE-CODE
 * @author Junior Lima - oojuniin@outlook.com
 * @since 29-08-2026
 */

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

    Optional<RefreshToken> findByJti(UUID jti);

    List<RefreshToken> findByFamilyIdAndRevokedAtIsNull(UUID familyId);

    void deleteByExpiresAtBefore(LocalDateTime cutoff);
}
