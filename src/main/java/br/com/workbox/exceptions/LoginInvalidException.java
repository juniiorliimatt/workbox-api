package br.com.workbox.exceptions;

public class LoginInvalidException extends RuntimeException {
    public LoginInvalidException(String message) {
        super(message);
    }
}
