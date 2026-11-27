package com.facochico.edu.domain.homework.exception;

public final class DuplicateSubmissionException extends RuntimeException {

    public DuplicateSubmissionException(long assignmentId, long studentId, Throwable cause) {
        super("Student %d already submitted assignment %d".formatted(studentId, assignmentId), cause);
    }
}
