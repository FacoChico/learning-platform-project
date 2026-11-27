package com.facochico.edu.domain.course.exception;

public final class CatalogEntryInUseException extends RuntimeException {

    public CatalogEntryInUseException(String resource, long id, Throwable cause) {
        super("%s %d is used by a course".formatted(resource, id), cause);
    }
}
