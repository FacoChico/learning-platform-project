package com.facochico.edu.domain.enrollment.exception;

public final class DuplicateEnrollmentException extends RuntimeException {

    public DuplicateEnrollmentException(long courseId, long studentId, Throwable cause) {
        super("Student %d is already enrolled in course %d".formatted(studentId, courseId), cause);
    }
}
