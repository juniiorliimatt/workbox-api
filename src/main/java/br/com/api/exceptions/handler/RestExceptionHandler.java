package br.com.api.exceptions.handler;

import br.com.api.exceptions.DatabaseException;
import br.com.api.exceptions.InvalidTokenException;
import br.com.api.exceptions.LoginInvalidException;
import br.com.api.exceptions.ResourceNotFoundException;
import br.com.api.exceptions.models.ErrorResponse;
import br.com.api.exceptions.models.FieldError;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.time.Instant;
import java.util.List;

@ControllerAdvice
public class RestExceptionHandler {

    private static final HttpStatus BAD_REQUEST = HttpStatus.BAD_REQUEST;
    private static final HttpStatus NOT_FOUND = HttpStatus.NOT_FOUND;

    @ExceptionHandler(JwtException.class)
    public ResponseEntity<ErrorResponse> handlerJwtException(final JwtException exception, final HttpServletRequest request) {
        final var errorResponse = ErrorResponse
                .builder()
                .timestamp(Instant.now())
                .status(HttpStatus.NOT_FOUND.value())
                .statusName(BAD_REQUEST.name())
                .message(exception.getMessage())
                .path(request.getRequestURI())
                .exception(exception.getClass().getSimpleName())
                .build();

        return ResponseEntity.status(BAD_REQUEST).body(errorResponse);
    }

    @ExceptionHandler(InvalidTokenException.class)
    public ResponseEntity<ErrorResponse> handlerInvalidTokenException(final InvalidTokenException exception, final HttpServletRequest request) {
        final var errorResponse = ErrorResponse
                .builder()
                .timestamp(Instant.now())
                .status(BAD_REQUEST.value())
                .statusName(BAD_REQUEST.name())
                .message(exception.getMessage())
                .path(request.getRequestURI())
                .exception(exception.getClass().getSimpleName())
                .build();

        return ResponseEntity.status(BAD_REQUEST).body(errorResponse);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handlerResourceNotFoundException(final ResourceNotFoundException exception, final HttpServletRequest request) {
        final var errorResponse = ErrorResponse
                .builder()
                .timestamp(Instant.now())
                .status(HttpStatus.NOT_FOUND.value())
                .statusName(NOT_FOUND.name())
                .message(exception.getMessage())
                .path(request.getRequestURI())
                .exception(exception.getClass().getSimpleName())
                .build();

        return ResponseEntity.status(NOT_FOUND).body(errorResponse);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValid(final MethodArgumentNotValidException exception, final HttpServletRequest request) {
        final BindingResult bindingResult = exception.getBindingResult();
        final List<FieldError> fieldErrors = bindingResult.getFieldErrors()
                .stream()
                .map(error -> {
                    final FieldError fieldError = new FieldError();
                    fieldError.setErrorCode(error.getCode());
                    fieldError.setField(error.getField());
                    return fieldError;
                }).toList();

        final var errorResponse = ErrorResponse
                .builder()
                .timestamp(Instant.now())
                .status(BAD_REQUEST.value())
                .statusName(BAD_REQUEST.name())
                .message(exception.getMessage())
                .path(request.getRequestURI())
                .exception(exception.getClass().getSimpleName())
                .fieldErrors(fieldErrors)
                .build();

        return ResponseEntity.status(BAD_REQUEST).body(errorResponse);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolationException(final ConstraintViolationException exception, final HttpServletRequest request) {
        final var constraintViolations = exception.getConstraintViolations();
        final List<FieldError> fieldErrors = constraintViolations
                .stream()
                .map(error -> {
                    final FieldError fieldError = new FieldError();
                    fieldError.setField(error.getPropertyPath().toString());
                    fieldError.setErrorCode(error.getMessage());
                    return fieldError;
                }).toList();

        final var errorResponse = ErrorResponse
                .builder()
                .timestamp(Instant.now())
                .status(BAD_REQUEST.value())
                .statusName(BAD_REQUEST.name())
                .path(request.getRequestURI())
                .exception(exception.getClass().getSimpleName())
                .fieldErrors(fieldErrors)
                .build();

        return ResponseEntity.status(BAD_REQUEST).body(errorResponse);
    }

    @ExceptionHandler(DatabaseException.class)
    public ResponseEntity<ErrorResponse> handleDatabaseException(final DatabaseException exception, final HttpServletRequest request) {
        return getErrorResponseEntity(request, exception.getMessage(), exception.getClass().getSimpleName());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrityViolationException(final DataIntegrityViolationException exception, final HttpServletRequest request) {
        return getErrorResponseEntity(request, exception.getMessage(), exception.getClass().getSimpleName());
    }

    @ExceptionHandler(LoginInvalidException.class)
    public ResponseEntity<ErrorResponse> handleLoginInvalidExceptionException(final LoginInvalidException exception, final HttpServletRequest request) {
        return getErrorResponseEntity(request, exception.getMessage(), exception.getClass().getSimpleName());
    }

    private ResponseEntity<ErrorResponse> getErrorResponseEntity(final HttpServletRequest request, final String message, final String simpleName) {
        final var errorResponse = ErrorResponse.builder()
                .timestamp(Instant.now())
                .status(BAD_REQUEST.value())
                .statusName(BAD_REQUEST.name())
                .message(message)
                .path(request.getRequestURI())
                .exception(simpleName)
                .build();

        return ResponseEntity.status(BAD_REQUEST).body(errorResponse);
    }
}
