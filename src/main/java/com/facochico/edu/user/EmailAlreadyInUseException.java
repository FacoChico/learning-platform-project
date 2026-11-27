package com.facochico.edu.user;

public class EmailAlreadyInUseException extends RuntimeException {

    public EmailAlreadyInUseException(Throwable cause) {
        super("Email is already in use", cause);
    }
}
