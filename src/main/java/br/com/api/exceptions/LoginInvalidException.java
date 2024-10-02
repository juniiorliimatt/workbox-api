package br.com.api.exceptions;

public class LoginInvalidException extends RuntimeException {
    public LoginInvalidException(String message) {
        super(message);
    }
}
