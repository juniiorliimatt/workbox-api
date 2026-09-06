package br.com.workbox.security.entities;

/**
 * Mecanismo de autenticação que um {@link ApiClient} está autorizado a usar. Hoje só
 * {@code CLIENT_SECRET} tem código de validação de fato (ver
 * {@code ApiClientUserDetailsService} + {@code SecurityConfig#introspectionFilterChain},
 * HTTP Basic client_id/client_secret) — os demais valores existem só como espaço
 * reservado no schema: adicionar um mecanismo novo (ex.: {@code AUTHORIZATION_CODE},
 * token estático) sempre vai exigir código novo de validação além da entrada na tabela,
 * a tabela só declara o que cada cliente PODE usar.
 *
 * @author CLAUDE-CODE
 * @author Junior Lima - oojuniin@outlook.com
 * @since 06-09-2026
 */
public enum ApiClientGrantType {
    CLIENT_SECRET
}
