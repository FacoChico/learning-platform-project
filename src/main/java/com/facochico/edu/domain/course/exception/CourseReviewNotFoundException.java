package com.facochico.edu.domain.course.exception;

public final class CourseReviewNotFoundException extends RuntimeException {

    public CourseReviewNotFoundException(long id) {
        super("Course review %d was not found".formatted(id));
    }
}
