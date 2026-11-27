package com.facochico.edu.domain.homework.exception;

public final class HomeworkNotFoundException extends RuntimeException {

    public HomeworkNotFoundException(String resource, long id) {
        super("%s %d was not found".formatted(resource, id));
    }
}
