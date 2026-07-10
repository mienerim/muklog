package com.nomlog.backend.auth;

public class InvalidGoogleTokenException extends RuntimeException {

    public InvalidGoogleTokenException(String message) {
        super(message);
    }
}
