package br.com.workbox.exceptions;

public class DatabaseException extends RuntimeException {
    public DatabaseException(String message) {
        super(message);
    }
}
