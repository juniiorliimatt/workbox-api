package br.com.workbox.exceptions.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import br.com.workbox.exceptions.DatabaseException;
import br.com.workbox.exceptions.InvalidTokenException;
import br.com.workbox.exceptions.LoginInvalidException;
import br.com.workbox.exceptions.ResourceNotFoundException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;
import java.util.List;
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

    private static final String PATH = "/api/auth/login";

    private RestExceptionHandler handler;
    private HttpServletRequest request;

    @BeforeEach
    void setUp() {
        handler = new RestExceptionHandler();
        request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn(PATH);
    }

    @Test
    @DisplayName("JwtException vira 400 com o corpo padrão")
    void jwtException() {
        final var response = handler.handlerJwtException(new JwtException("expired"), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().getMessage()).isEqualTo("expired");
        assertThat(response.getBody().getPath()).isEqualTo(PATH);
        assertThat(response.getBody().getException()).isEqualTo("JwtException");
    }

    @Test
    @DisplayName("InvalidTokenException vira 400")
    void invalidTokenException() {
        final var response = handler.handlerInvalidTokenException(
                new InvalidTokenException("Invalid token or expired"), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().getMessage()).isEqualTo("Invalid token or expired");
    }

    @Test
    @DisplayName("ResourceNotFoundException vira 404")
    void resourceNotFoundException() {
        final var response = handler.handlerResourceNotFoundException(
                new ResourceNotFoundException("User not found"), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody().getMessage()).isEqualTo("User not found");
        assertThat(response.getBody().getStatusName()).isEqualTo("NOT_FOUND");
    }

    @Test
    @DisplayName("MethodArgumentNotValidException vira 400 com os fieldErrors")
    void methodArgumentNotValidException() throws NoSuchMethodException {
        final var bindingResult = mock(BindingResult.class);
        final var fieldError = new org.springframework.validation.FieldError("dto", "username", "must not be blank");
        when(bindingResult.getFieldErrors()).thenReturn(List.of(fieldError));
        final var methodParameter = new MethodParameter(
                DummyValidatedEndpoint.class.getDeclaredMethod("handle", String.class), 0);
        final var exception = new MethodArgumentNotValidException(methodParameter, bindingResult);

        final var response = handler.handleMethodArgumentNotValid(exception, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().getFieldErrors()).hasSize(1);
        assertThat(response.getBody().getFieldErrors().get(0).getField()).isEqualTo("username");
    }

    @Test
    @DisplayName("ConstraintViolationException vira 400 com os fieldErrors")
    void constraintViolationException() {
        final ConstraintViolation<?> violation = mock(ConstraintViolation.class);
        final var path = mock(Path.class);
        when(path.toString()).thenReturn("username");
        when(violation.getPropertyPath()).thenReturn(path);
        when(violation.getMessage()).thenReturn("must not be blank");
        final Set<ConstraintViolation<?>> violations = Set.of(violation);
        final var exception = new ConstraintViolationException(violations);

        final var response = handler.handleConstraintViolationException(exception, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().getFieldErrors()).hasSize(1);
        assertThat(response.getBody().getFieldErrors().get(0).getField()).isEqualTo("username");
    }

    @Test
    @DisplayName("DatabaseException vira 400 com mensagem genérica — nunca ecoa a mensagem real da exceção")
    void databaseExceptionUsesGenericMessage() {
        final var response = handler.handleDatabaseException(
                new DatabaseException("constraint fk_users_role violated on table internal_x"), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().getMessage()).isEqualTo("Database error");
        assertThat(response.getBody().getMessage()).doesNotContain("fk_users_role", "internal_x");
    }

    @Test
    @DisplayName("DataIntegrityViolationException vira 400 com mensagem genérica — nunca ecoa detalhe de schema")
    void dataIntegrityViolationUsesGenericMessage() {
        final var response = handler.handleDataIntegrityViolationException(
                new DataIntegrityViolationException("duplicate key value violates unique constraint \"users_api_username_key\""),
                request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().getMessage()).isEqualTo("Data integrity violation");
        assertThat(response.getBody().getMessage()).doesNotContain("users_api_username_key");
    }

    @Test
    @DisplayName("LoginInvalidException vira 400 preservando a mensagem original")
    void loginInvalidException() {
        final var response = handler.handleLoginInvalidExceptionException(
                new LoginInvalidException("credenciais inválidas"), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().getMessage()).isEqualTo("credenciais inválidas");
    }

    @SuppressWarnings("unused")
    private static final class DummyValidatedEndpoint {
        void handle(String username) {
        }
    }
}
