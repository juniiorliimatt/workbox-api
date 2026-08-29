package br.com.workbox.security.entities;

import br.com.workbox.security.dto.UserApiInsertOrUpdateDTO;
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

    // Unicidade real é um índice único parcial (WHERE deleted_at IS NULL, ver
    // changelog v0.0.2) — exclusão é lógica, então UNIQUE bruto na coluna impediria
    // reusar o username de um usuário deletado. Não declarar unique=true aqui.
    @Size(min = 5, max = 50)
    @Column(nullable = false)
    @NotBlank(message = "Username is mandatory")
    private String username;

    @Column(nullable = false)
    @NotBlank(message = "password is mandatory")
    @NotBlank(message = "Required field")
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String password;

    // Mesma razão do username acima: unicidade é índice parcial, não coluna.
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
        this.username = dto.username();
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

    @PrePersist
    public void prePersist() {
        this.isAccountNonExpired = Objects.isNull(isAccountNonExpired) ? Boolean.TRUE : this.isAccountNonExpired;
        this.isAccountNonLocked = Objects.isNull(isAccountNonLocked) ? Boolean.TRUE : this.isAccountNonLocked;
        this.isCredentialsNonExpired = Objects.isNull(isCredentialsNonExpired) ? Boolean.TRUE : this.isCredentialsNonExpired;
        this.isEnabled = Objects.isNull(isEnabled) ? Boolean.TRUE : this.isEnabled;
        this.tokenVersion = Objects.isNull(tokenVersion) ? 0L : this.tokenVersion;
        this.failedLoginAttempts = Objects.isNull(failedLoginAttempts) ? 0 : this.failedLoginAttempts;
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
