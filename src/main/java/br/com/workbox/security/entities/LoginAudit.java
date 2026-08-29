package br.com.workbox.security.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * @author CLAUDE-CODE
 * @author Junior Lima - oojuniin@outlook.com
 * @since 29-08-2026
 */

@Getter
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "login_audit")
public class LoginAudit {

    @Id
    private UUID id;

    @Column(nullable = false)
    private String username;

    @Column(nullable = false)
    private boolean successful;

    private String reason;

    private String ipAddress;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    public static LoginAudit of(String username, boolean successful, String reason, String ipAddress) {
        return LoginAudit.builder()
                .id(UUID.randomUUID())
                .username(username)
                .successful(successful)
                .reason(reason)
                .ipAddress(ipAddress)
                .createdAt(LocalDateTime.now())
                .build();
    }
}
