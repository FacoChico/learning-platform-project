package com.facochico.edu.domain.homework.exception;

public final class SubmissionDeadlinePassedException extends RuntimeException {

    public SubmissionDeadlinePassedException(long assignmentId) {
        super("The deadline for assignment %d has passed".formatted(assignmentId));
    }
}
