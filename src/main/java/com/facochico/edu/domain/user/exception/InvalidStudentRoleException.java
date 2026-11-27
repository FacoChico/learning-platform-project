package com.facochico.edu.domain.user.exception;

public final class InvalidStudentRoleException extends RuntimeException {

    public InvalidStudentRoleException() {
        super("User must have the STUDENT role");
    }
}
