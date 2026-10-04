package br.com.workbox.security.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Módulo do sistema (Finanças, Forza...) ao qual uma {@link Role} pode dar acesso.
 * Catálogo fechado: só migrations criam/alteram módulos — não há CRUD na API. Por isso
 * não é {@code @Audited} nem tem auditoria de criação/edição.
 *
 * <p>Nome {@code AppModule} (e não {@code Module}) pra não colidir com
 * {@link java.lang.Module}.
 *
 * @author CLAUDE-CODE
 * @author Junior Lima - oojuniin@outlook.com
 * @since 03-10-2026
 */

@Entity
@Getter
@Table(name = "modules")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AppModule implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(nullable = false, unique = true, updatable = false)
    private String code;

    @NotBlank
    @Column(nullable = false)
    private String name;
}
