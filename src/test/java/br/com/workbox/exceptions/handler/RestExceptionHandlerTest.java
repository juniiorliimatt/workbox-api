package br.com.workbox.exceptions.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import br.com.workbox.exceptions.DatabaseException;
import br.com.workbox.exceptions.InvalidRefreshTokenException;
import br.com.workbox.exceptions.InvalidTokenException;
import br.com.workbox.exceptions.LoginInvalidException;
import br.com.workbox.exceptions.ResourceNotFoundException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.MethodArgumentNotValidException;

class RestExceptionHandlerTest {

    private RestExceptionHandler handler;
    private HttpServletRequest request;

    @BeforeEach
    void setUp() {
        handler = new RestExceptionHandler();
        request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/api/auth/login");
    }

    @Test
    @DisplayName("JwtException vira 400 com o detail da mensagem")
    void jwtException() {
        final var response = handler.handleJwtException(new JwtException("expired"));

        assertThat(response.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(response.getDetail()).isEqualTo("expired");
    }

    @Test
    @DisplayName("InvalidTokenException vira 400")
    void invalidTokenException() {
        final var response = handler.handleInvalidTokenException(new InvalidTokenException("Invalid token or expired"));

        assertThat(response.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(response.getDetail()).isEqualTo("Invalid token or expired");
    }

    @Test
    @DisplayName("InvalidRefreshTokenException vira 401 com mensagem genérica — nunca ecoa o motivo interno")
    void invalidRefreshTokenException() {
        final var response = handler.handleInvalidRefreshTokenException(new InvalidRefreshTokenException("Refresh token reuse detected"));

        assertThat(response.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
        assertThat(response.getDetail()).isEqualTo("Invalid or expired refresh token");
    }

    @Test
    @DisplayName("ResourceNotFoundException vira 404")
    void resourceNotFoundException() {
        final var response = handler.handleResourceNotFoundException(new ResourceNotFoundException("User not found"));

        assertThat(response.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
        assertThat(response.getDetail()).isEqualTo("User not found");
    }

    @Test
    @DisplayName("MethodArgumentNotValidException vira 400 com os errors")
    void methodArgumentNotValidException() throws NoSuchMethodException {
        final var bindingResult = mock(BindingResult.class);
        final var fieldError = new org.springframework.validation.FieldError("dto", "username", "must not be blank");
        when(bindingResult.getFieldErrors()).thenReturn(List.of(fieldError));
        final var methodParameter = new MethodParameter(
                DummyValidatedEndpoint.class.getDeclaredMethod("handle", String.class), 0);
        final var exception = new MethodArgumentNotValidException(methodParameter, bindingResult);

        final var response = handler.handleMethodArgumentNotValid(exception);

        assertThat(response.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        @SuppressWarnings("unchecked")
        final var errors = (List<Map<String, String>>) response.getProperties().get("errors");
        assertThat(errors).hasSize(1);
        assertThat(errors.get(0).get("field")).isEqualTo("username");
    }

    @Test
    @DisplayName("ConstraintViolationException vira 400 com os errors")
    void constraintViolationException() {
        final ConstraintViolation<?> violation = mock(ConstraintViolation.class);
        final var path = mock(Path.class);
        when(path.toString()).thenReturn("username");
        when(violation.getPropertyPath()).thenReturn(path);
        when(violation.getMessage()).thenReturn("must not be blank");
        final Set<ConstraintViolation<?>> violations = Set.of(violation);
        final var exception = new ConstraintViolationException(violations);

        final var response = handler.handleConstraintViolationException(exception);

        assertThat(response.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        @SuppressWarnings("unchecked")
        final var errors = (List<Map<String, String>>) response.getProperties().get("errors");
        assertThat(errors).hasSize(1);
        assertThat(errors.get(0).get("field")).isEqualTo("username");
    }

    @Test
    @DisplayName("DatabaseException vira 500 com mensagem genérica — nunca ecoa a mensagem real da exceção")
    void databaseExceptionUsesGenericMessage() {
        final var response = handler.handleDatabaseException(
                new DatabaseException("constraint fk_users_role violated on table internal_x"), request);

        assertThat(response.getStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.value());
        assertThat(response.getDetail()).isEqualTo("Database error");
        assertThat(response.getDetail()).doesNotContain("fk_users_role", "internal_x");
    }

    @Test
    @DisplayName("DataIntegrityViolationException vira 409 com mensagem genérica — nunca ecoa detalhe de schema")
    void dataIntegrityViolationUsesGenericMessage() {
        final var response = handler.handleDataIntegrityViolationException(
                new DataIntegrityViolationException("duplicate key value violates unique constraint \"users_api_username_key\""),
                request);

        assertThat(response.getStatus()).isEqualTo(HttpStatus.CONFLICT.value());
        assertThat(response.getDetail()).isEqualTo("Data integrity violation");
        assertThat(response.getDetail()).doesNotContain("users_api_username_key");
    }

    @Test
    @DisplayName("LoginInvalidException vira 400 preservando a mensagem original")
    void loginInvalidException() {
        final var response = handler.handleLoginInvalidException(new LoginInvalidException("credenciais inválidas"));

        assertThat(response.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(response.getDetail()).isEqualTo("credenciais inválidas");
    }

    @Test
    @DisplayName("Exceção não mapeada vira 500 genérico — nunca ecoa a mensagem/stack real")
    void unexpectedExceptionUsesGenericMessage() {
        final var response = handler.handleUnexpected(new IllegalStateException("NPE em algum lugar sensível"), request);

        assertThat(response.getStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.value());
        assertThat(response.getDetail()).isEqualTo("Unexpected error");
        assertThat(response.getDetail()).doesNotContain("NPE", "sensível");
    }

    @SuppressWarnings("unused")
    private static final class DummyValidatedEndpoint {
        void handle(String username) {
        }
    }
}
