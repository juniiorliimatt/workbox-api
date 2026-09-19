package br.com.workbox.exceptions.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import br.com.workbox.exceptions.DatabaseException;
import br.com.workbox.exceptions.InvalidImageException;
import br.com.workbox.exceptions.InvalidRefreshTokenException;
import br.com.workbox.exceptions.InvalidRequestException;
import br.com.workbox.exceptions.InvalidTokenException;
import br.com.workbox.exceptions.LoginInvalidException;
import br.com.workbox.exceptions.ResourceNotFoundException;
import br.com.workbox.exceptions.UserAlreadyExistsException;
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
import org.springframework.context.support.MessageSourceAccessor;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.core.MethodParameter;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindingResult;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

class RestExceptionHandlerTest {

    private RestExceptionHandler handler;
    private HttpServletRequest request;

    @BeforeEach
    void setUp() {
        final var messageSource = new ResourceBundleMessageSource();
        messageSource.setBasename("messages");
        messageSource.setDefaultEncoding("UTF-8");
        final var messages = new MessageSourceAccessor(messageSource, java.util.Locale.of("pt", "BR"));
        handler = new RestExceptionHandler(messages);
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
        assertThat(response.getDetail()).isEqualTo("Refresh token inválido ou expirado");
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
        final var fieldError = new org.springframework.validation.FieldError("dto", "email", "must not be blank");
        when(bindingResult.getFieldErrors()).thenReturn(List.of(fieldError));
        final var methodParameter = new MethodParameter(
                DummyValidatedEndpoint.class.getDeclaredMethod("handle", String.class), 0);
        final var exception = new MethodArgumentNotValidException(methodParameter, bindingResult);

        final var response = handler.handleMethodArgumentNotValid(exception);

        assertThat(response.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        @SuppressWarnings("unchecked")
        final var errors = (List<Map<String, String>>) response.getProperties().get("errors");
        assertThat(errors).hasSize(1);
        assertThat(errors.get(0).get("field")).isEqualTo("email");
    }

    @Test
    @DisplayName("ConstraintViolationException vira 400 com os errors")
    void constraintViolationException() {
        final ConstraintViolation<?> violation = mock(ConstraintViolation.class);
        final var path = mock(Path.class);
        when(path.toString()).thenReturn("email");
        when(violation.getPropertyPath()).thenReturn(path);
        when(violation.getMessage()).thenReturn("must not be blank");
        final Set<ConstraintViolation<?>> violations = Set.of(violation);
        final var exception = new ConstraintViolationException(violations);

        final var response = handler.handleConstraintViolationException(exception);

        assertThat(response.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        @SuppressWarnings("unchecked")
        final var errors = (List<Map<String, String>>) response.getProperties().get("errors");
        assertThat(errors).hasSize(1);
        assertThat(errors.get(0).get("field")).isEqualTo("email");
    }

    @Test
    @DisplayName("DatabaseException vira 500 com mensagem genérica — nunca ecoa a mensagem real da exceção")
    void databaseExceptionUsesGenericMessage() {
        final var response = handler.handleDatabaseException(
                new DatabaseException("constraint fk_users_role violated on table internal_x"), request);

        assertThat(response.getStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.value());
        assertThat(response.getDetail()).isEqualTo("Erro de banco de dados");
        assertThat(response.getDetail()).doesNotContain("fk_users_role", "internal_x");
    }

    @Test
    @DisplayName("DataIntegrityViolationException vira 409 com mensagem genérica — nunca ecoa detalhe de schema")
    void dataIntegrityViolationUsesGenericMessage() {
        final var response = handler.handleDataIntegrityViolationException(
                new DataIntegrityViolationException("duplicate key value violates unique constraint \"idx_users_api_email_active\""),
                request);

        assertThat(response.getStatus()).isEqualTo(HttpStatus.CONFLICT.value());
        assertThat(response.getDetail()).isEqualTo("Violação de integridade referencial");
        assertThat(response.getDetail()).doesNotContain("idx_users_api_email_active");
    }

    @Test
    @DisplayName("InvalidRequestException vira 400 preservando a mensagem original")
    void invalidRequestException() {
        final var response = handler.handleInvalidRequestException(new InvalidRequestException("Role id is required"));

        assertThat(response.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(response.getDetail()).isEqualTo("Role id is required");
    }

    @Test
    @DisplayName("UserAlreadyExistsException vira 409 preservando a mensagem original")
    void userAlreadyExistsException() {
        final var response = handler.handleUserAlreadyExistsException(new UserAlreadyExistsException("Email already in use"));

        assertThat(response.getStatus()).isEqualTo(HttpStatus.CONFLICT.value());
        assertThat(response.getDetail()).isEqualTo("Email already in use");
    }

    @Test
    @DisplayName("InvalidImageException vira 400 preservando a mensagem original")
    void invalidImageException() {
        final var response = handler.handleInvalidImageException(new InvalidImageException("File is not a valid image"));

        assertThat(response.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(response.getDetail()).isEqualTo("File is not a valid image");
    }

    @Test
    @DisplayName("MaxUploadSizeExceededException vira 413 com mensagem genérica")
    void maxUploadSizeExceededException() {
        final var response = handler.handleMaxUploadSizeExceededException(new MaxUploadSizeExceededException(2_000_000L));

        assertThat(response.getStatus()).isEqualTo(HttpStatus.PAYLOAD_TOO_LARGE.value());
        assertThat(response.getDetail()).isEqualTo("Arquivo excede o tamanho máximo permitido");
    }

    @Test
    @DisplayName("LoginInvalidException vira 400 preservando a mensagem original")
    void loginInvalidException() {
        final var response = handler.handleLoginInvalidException(new LoginInvalidException("credenciais inválidas"));

        assertThat(response.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(response.getDetail()).isEqualTo("credenciais inválidas");
    }

    @Test
    @DisplayName("HttpMessageNotReadableException (JSON malformado) vira 400, não 500")
    void httpMessageNotReadableException() {
        final var exception = new HttpMessageNotReadableException("Cannot deserialize value", (org.springframework.http.HttpInputMessage) null);

        final var response = handler.handleMessageNotReadable(exception);

        assertThat(response.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(response.getDetail()).isEqualTo("Corpo da requisição JSON mal formado");
    }

    @Test
    @DisplayName("NoResourceFoundException (rota sem handler) vira 404, não 500")
    void noResourceFoundException() {
        final var exception = new NoResourceFoundException(org.springframework.http.HttpMethod.GET, "api/v1/revenues");

        final var response = handler.handleNoResourceFound(exception);

        assertThat(response.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
        assertThat(response.getDetail()).isEqualTo("Nenhum handler para essa rota");
    }

    @Test
    @DisplayName("Exceção não mapeada vira 500 genérico — nunca ecoa a mensagem/stack real")
    void unexpectedExceptionUsesGenericMessage() {
        final var response = handler.handleUnexpected(new IllegalStateException("NPE em algum lugar sensível"), request);

        assertThat(response.getStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.value());
        assertThat(response.getDetail()).isEqualTo("Erro inesperado");
        assertThat(response.getDetail()).doesNotContain("NPE", "sensível");
    }

    @SuppressWarnings("unused")
    private static final class DummyValidatedEndpoint {
        void handle(final String username) {
        }
    }
}
