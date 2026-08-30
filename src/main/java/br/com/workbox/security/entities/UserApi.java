package br.com.workbox.security.entities;

import br.com.workbox.security.dto.UserApiInsertOrUpdateDTO;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * @author CLAUDE-CODE
 * @author Junior Lima - oojuniin@outlook.com
 * @since 29-08-2026
 */

@Getter
@Setter
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "users_api")
@Builder
@EntityListeners(AuditingEntityListener.class)
@SQLRestriction("deleted_at IS NULL")
@Audited
public class UserApi implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // Nome social — como o usuário quer ser chamado, usado pro front exibir. Livre, não é
    // identificador de login e não precisa ser único.
    @Size(min = 2, max = 120)
    @Column(nullable = false)
    @NotBlank(message = "Social name is mandatory")
    private String socialName;

    @Column(nullable = false)
    @NotBlank(message = "password is mandatory")
    @NotBlank(message = "Required field")
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String password;

    // Identificador de login (ver getUsername() abaixo) — unicidade real é um índice
    // único parcial (WHERE deleted_at IS NULL, ver changelog v0.0.2), exclusão lógica
    // impediria reusar o email de um usuário deletado com UNIQUE bruto na coluna.
    @Column(nullable = false)
    @NotBlank(message = "Email is mandatory")
    @Email(message = "Email must be valid")
    private String email;

    @NotAudited
    @ManyToMany(fetch = FetchType.EAGER, cascade = {CascadeType.DETACH, CascadeType.MERGE, CascadeType.PERSIST, CascadeType.REFRESH})
    @JoinTable(name = "user_roles",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id"))
    private Set<Role> roles = new HashSet<>();

    @Column(nullable = false)
    private Boolean isEnabled;

    private Boolean isAccountNonExpired;

    private Boolean isAccountNonLocked;

    private Boolean isCredentialsNonExpired;

    @Column(nullable = false)
    private Long tokenVersion;

    @Column(nullable = false)
    private Integer failedLoginAttempts;

    // Lockout automático por brute-force: NULL = não bloqueado, no futuro = bloqueado
    // até esse instante. Combinado com isAccountNonLocked em isAccountNonLocked().
    private LocalDateTime lockedUntil;

    // Segredo TOTP (Base32) — texto plano por simplicidade de projeto de estudo; um
    // ambiente real guardaria isso cifrado em repouso (KMS/envelope encryption), nunca
    // como coluna legível direto no banco. NULL até o primeiro /mfa/enroll. NotAudited:
    // é segredo vivo, não faz sentido duplicar histórico dele numa tabela de auditoria.
    @NotAudited
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String mfaSecret;

    @Column(nullable = false)
    private Boolean mfaEnabled;

    // Nome do arquivo no disco (uploads/avatars), gerado pelo servidor (UUID) — nunca o
    // nome original enviado pelo client, pra não abrir path traversal (CWE-22). NULL até o
    // primeiro upload. Detalhe de implementação: nunca serializado (o client usa
    // UserApiDTO.avatarUrl, que aponta pro endpoint que serve os bytes).
    @JsonIgnore
    private String avatarFilename;

    private LocalDateTime deletedAt;

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    @CreatedBy
    @Column(updatable = false)
    private String createdBy;

    @LastModifiedBy
    private String updatedBy;

    public UserApi(UserApiInsertOrUpdateDTO dto) {
        this.socialName = dto.socialName();
        this.password = dto.password();
        this.email = dto.email();
        this.isEnabled = dto.isEnabled();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return roles.stream()
                .map(role -> new SimpleGrantedAuthority(role.getAuthority()))
                .toList();
    }

    /**
     * Identificador de autenticação do Spring Security — login é por email, não por
     * username. {@code @JsonIgnore} porque Jackson trata todo getter público como
     * propriedade serializável, e sem isso esse método sintético do contrato
     * {@link UserDetails} vazava um campo "username" duplicando o valor de email em
     * qualquer serialização da entidade (ex.: schema OpenAPI).
     */
    @Override
    @JsonIgnore
    public String getUsername() {
        return email;
    }

    @PrePersist
    public void prePersist() {
        this.isAccountNonExpired = Objects.isNull(isAccountNonExpired) ? Boolean.TRUE : this.isAccountNonExpired;
        this.isAccountNonLocked = Objects.isNull(isAccountNonLocked) ? Boolean.TRUE : this.isAccountNonLocked;
        this.isCredentialsNonExpired = Objects.isNull(isCredentialsNonExpired) ? Boolean.TRUE : this.isCredentialsNonExpired;
        this.isEnabled = Objects.isNull(isEnabled) ? Boolean.TRUE : this.isEnabled;
        this.tokenVersion = Objects.isNull(tokenVersion) ? 0L : this.tokenVersion;
        this.failedLoginAttempts = Objects.isNull(failedLoginAttempts) ? 0 : this.failedLoginAttempts;
        this.mfaEnabled = Objects.isNull(mfaEnabled) ? Boolean.FALSE : this.mfaEnabled;
    }

    @Override
    public boolean isEnabled() {
        return isEnabled;
    }

    @Override
    public boolean isAccountNonExpired() {
        return isAccountNonExpired;
    }

    @Override
    public boolean isAccountNonLocked() {
        return isAccountNonLocked && (lockedUntil == null || lockedUntil.isBefore(LocalDateTime.now()));
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return isCredentialsNonExpired;
    }
}
