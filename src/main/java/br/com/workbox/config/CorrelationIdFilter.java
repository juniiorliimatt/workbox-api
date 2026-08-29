package br.com.workbox.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.lang.NonNull;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Propaga (ou gera) um {@code X-Request-Id} por requisição, via MDC — necessário pra
 * correlacionar uma requisição através de logs do workbox-api e de qualquer resource
 * server downstream (ex.: budget-service) que reencaminhe o mesmo header. Registrado
 * manualmente na {@code SecurityFilterChain} (ver SecurityConfig), igual ao JwtService —
 * não é {@code @Component}: um filtro solto no contexto é auto-coletado pelo
 * autoconfig do MockMvc em teste, cujo {@code MockMvcFilterDecorator.init()} quebra com
 * NPE (bug conhecido do Spring Test com {@code GenericFilterBean} fora da chain de
 * Security). Roda antes do JwtService pra que autenticação malformada/rejeitada também
 * saia com requestId no log.
 *
 * @author CLAUDE-CODE
 * @author Junior Lima - oojuniin@outlook.com
 * @since 29-08-2026
 */
public class CorrelationIdFilter extends OncePerRequestFilter {

    public static final String REQUEST_ID_HEADER = "X-Request-Id";
    public static final String MDC_KEY = "requestId";

    @Override
    protected void doFilterInternal(final HttpServletRequest request, @NonNull final HttpServletResponse response,
                                     @NonNull final FilterChain filterChain) throws ServletException, IOException {
        final var incoming = request.getHeader(REQUEST_ID_HEADER);
        final var requestId = (incoming == null || incoming.isBlank()) ? UUID.randomUUID().toString() : incoming;

        MDC.put(MDC_KEY, requestId);
        response.setHeader(REQUEST_ID_HEADER, requestId);
        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(MDC_KEY);
        }
    }
}
