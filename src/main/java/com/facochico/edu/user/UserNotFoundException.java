package com.facochico.edu.user;

public class UserNotFoundException extends RuntimeException {

    UserNotFoundException(long id) {
        super("User %d was not found".formatted(id));
    }
}
