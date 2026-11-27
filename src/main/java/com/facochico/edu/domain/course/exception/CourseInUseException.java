package com.facochico.edu.domain.course.exception;

public final class CourseInUseException extends RuntimeException {

    public CourseInUseException(long id, Throwable cause) {
        super("Course %d has enrollments".formatted(id), cause);
    }
}
