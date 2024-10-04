package br.com.api.security.entities;

import br.com.api.security.dto.UserApiInsertOrUpdateDTO;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.*;

@Getter
@Setter
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "users_api")
@EntityListeners(AuditingEntityListener.class)
public class UserApi implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Size(min = 5, max = 50)
    @Column(nullable = false, unique = true)
    @NotBlank(message = "Username is mandatory")
    private String username;

    @Column(nullable = false)
    @NotBlank(message = "password is mandatory")
    @NotBlank(message = "Required field")
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String password;

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
        this.isAccountNonExpired = Objects.isNull(isAccountNonExpired) ? Boolean.TRUE : this.isCredentialsNonExpired;
        this.isAccountNonLocked = Objects.isNull(isAccountNonLocked) ? Boolean.TRUE : this.isAccountNonLocked;
        this.isCredentialsNonExpired = Objects.isNull(isCredentialsNonExpired) ? Boolean.TRUE : this.isCredentialsNonExpired;
        this.isEnabled = Objects.isNull(isEnabled) ? Boolean.TRUE : this.isEnabled;
    }

    @Override
    public boolean isEnabled() {
        return isEnabled;
    }
}
