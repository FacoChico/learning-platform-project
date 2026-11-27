package com.facochico.edu.domain.course.exception;

public final class CurriculumItemNotFoundException extends RuntimeException {

    public CurriculumItemNotFoundException(String resource, long id) {
        super("%s %d was not found".formatted(resource, id));
    }
}
