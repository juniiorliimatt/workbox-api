package br.com.workbox.security.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * Cliente de serviço-a-serviço autorizado a consultar {@code POST /api/v1/auth/introspect}
 * (ex.: budget-service). Cadastro é 100% dado — nunca hardcoded em Java: liberar um
 * microserviço novo é inserir uma linha aqui (ver changelog Liquibase
 * {@code 260906_0000_create_table_api_clients.sql}), não editar {@code SecurityConfig}.
 *
 * @author CLAUDE-CODE
 * @author Junior Lima - oojuniin@outlook.com
 * @since 06-09-2026
 */

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "api_clients")
@EntityListeners(AuditingEntityListener.class)
public class ApiClient {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(name = "client_id", nullable = false)
    private String clientId;

    // Hash BCrypt — nunca o secret em texto plano. JsonIgnore por hábito de projeto
    // (nunca serializado hoje, essa entidade não tem DTO/endpoint de leitura ainda).
    @JsonIgnore
    @Column(name = "client_secret_hash", nullable = false)
    private String clientSecretHash;

    // Mapeamento JSON nativo do Hibernate 6 (JdbcTypeCode + SqlTypes.JSON) — Set<enum>
    // vira array JSON de nomes na coluna JSONB, sem biblioteca extra.
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "allowed_grant_types", nullable = false)
    private Set<ApiClientGrantType> allowedGrantTypes;

    @Column(nullable = false)
    private Boolean active;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @CreatedBy
    @Column(name = "created_by", updatable = false)
    private String createdBy;

    @LastModifiedBy
    @Column(name = "updated_by")
    private String updatedBy;
}
