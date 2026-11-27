package com.facochico.edu.domain.homework.exception;

public final class HomeworkInUseException extends RuntimeException {

    public HomeworkInUseException(String resource, long id, Throwable cause) {
        super("%s %d has submitted homework".formatted(resource, id), cause);
    }
}
