package com.facochico.edu.domain.user.exception;

public class EmailAlreadyInUseException extends RuntimeException {

    public EmailAlreadyInUseException(Throwable cause) {
        super("Email is already in use", cause);
    }
}
