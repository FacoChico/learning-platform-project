package com.facochico.edu.domain.user.exception;

public final class UserNotFoundException extends RuntimeException {

    public UserNotFoundException(long id) {
        super("User %d was not found".formatted(id));
    }
}
