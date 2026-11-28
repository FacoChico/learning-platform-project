package com.facochico.edu.domain.course.exception;

public final class CourseReviewAccessDeniedException extends RuntimeException {

    public CourseReviewAccessDeniedException() {
        super("Only the author may modify the review");
    }
}
