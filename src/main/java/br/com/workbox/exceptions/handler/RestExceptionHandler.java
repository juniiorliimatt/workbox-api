package br.com.workbox.exceptions.handler;

import br.com.workbox.exceptions.DatabaseException;
import br.com.workbox.exceptions.InvalidRefreshTokenException;
import br.com.workbox.exceptions.InvalidTokenException;
import br.com.workbox.exceptions.InvalidImageException;
import br.com.workbox.exceptions.InvalidRequestException;
import br.com.workbox.exceptions.LoginInvalidException;
import br.com.workbox.exceptions.ResourceNotFoundException;
import br.com.workbox.exceptions.UserAlreadyExistsException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

/**
 * Corpo de erro padronizado em RFC 7807 ({@link ProblemDetail}, suporte nativo do
 * Spring 6) — mesmo shape ({@code type/title/status/detail/instance} + extensões) pra
 * qualquer exceção, incluindo o {@link #handleUnexpected} catch-all: sem ele, qualquer
 * exceção não mapeada aqui caía no whitelabel error padrão do Spring, potencialmente
 * vazando stack trace (depende de {@code server.error.include-stacktrace}).
 *
 * @author CLAUDE-CODE
 * @author Junior Lima - oojuniin@outlook.com
 * @since 29-08-2026
 */
@RestControllerAdvice
public class RestExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(RestExceptionHandler.class);

    @ExceptionHandler(JwtException.class)
    public ProblemDetail handleJwtException(final JwtException exception) {
        return problem(HttpStatus.BAD_REQUEST, exception.getMessage());
    }

    @ExceptionHandler(InvalidTokenException.class)
    public ProblemDetail handleInvalidTokenException(final InvalidTokenException exception) {
        return problem(HttpStatus.BAD_REQUEST, exception.getMessage());
    }

    @ExceptionHandler(InvalidRefreshTokenException.class)
    public ProblemDetail handleInvalidRefreshTokenException(final InvalidRefreshTokenException exception) {
        return problem(HttpStatus.UNAUTHORIZED, "Invalid or expired refresh token");
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ProblemDetail handleResourceNotFoundException(final ResourceNotFoundException exception) {
        return problem(HttpStatus.NOT_FOUND, exception.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleMethodArgumentNotValid(final MethodArgumentNotValidException exception) {
        final var detail = problem(HttpStatus.BAD_REQUEST, "Validation failed");
        detail.setProperty("errors", exception.getBindingResult().getFieldErrors().stream()
                .map(error -> Map.of("field", error.getField(), "message", String.valueOf(error.getDefaultMessage())))
                .toList());
        return detail;
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ProblemDetail handleConstraintViolationException(final ConstraintViolationException exception) {
        final var detail = problem(HttpStatus.BAD_REQUEST, "Validation failed");
        detail.setProperty("errors", exception.getConstraintViolations().stream()
                .map(violation -> Map.of("field", violation.getPropertyPath().toString(), "message", violation.getMessage()))
                .toList());
        return detail;
    }

    @ExceptionHandler(DatabaseException.class)
    public ProblemDetail handleDatabaseException(final DatabaseException exception, final HttpServletRequest request) {
        logger.error("Database error on {}", request.getRequestURI(), exception);
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, "Database error");
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ProblemDetail handleDataIntegrityViolationException(final DataIntegrityViolationException exception, final HttpServletRequest request) {
        logger.error("Data integrity violation on {}", request.getRequestURI(), exception);
        return problem(HttpStatus.CONFLICT, "Data integrity violation");
    }

    @ExceptionHandler(InvalidImageException.class)
    public ProblemDetail handleInvalidImageException(final InvalidImageException exception) {
        return problem(HttpStatus.BAD_REQUEST, exception.getMessage());
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ProblemDetail handleMaxUploadSizeExceededException(final MaxUploadSizeExceededException exception) {
        return problem(HttpStatus.PAYLOAD_TOO_LARGE, "File exceeds the maximum allowed size");
    }

    @ExceptionHandler(UserAlreadyExistsException.class)
    public ProblemDetail handleUserAlreadyExistsException(final UserAlreadyExistsException exception) {
        return problem(HttpStatus.CONFLICT, exception.getMessage());
    }

    @ExceptionHandler(InvalidRequestException.class)
    public ProblemDetail handleInvalidRequestException(final InvalidRequestException exception) {
        return problem(HttpStatus.BAD_REQUEST, exception.getMessage());
    }

    @ExceptionHandler(LoginInvalidException.class)
    public ProblemDetail handleLoginInvalidException(final LoginInvalidException exception) {
        return problem(HttpStatus.BAD_REQUEST, exception.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnexpected(final Exception exception, final HttpServletRequest request) {
        logger.error("Unhandled exception on {}", request.getRequestURI(), exception);
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected error");
    }

    private ProblemDetail problem(final HttpStatus status, final String detail) {
        return ProblemDetail.forStatusAndDetail(status, detail);
    }
}
