package com.facochico.edu.domain.enrollment.exception;

public final class EnrollmentNotFoundException extends RuntimeException {

    public EnrollmentNotFoundException(long courseId, long studentId) {
        super("Enrollment for student %d in course %d was not found".formatted(studentId, courseId));
    }
}
