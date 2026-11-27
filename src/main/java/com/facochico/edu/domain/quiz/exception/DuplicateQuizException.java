package com.facochico.edu.domain.quiz.exception;

public final class DuplicateQuizException extends RuntimeException {

    public DuplicateQuizException(long moduleId) {
        super("Module %d already has a quiz".formatted(moduleId));
    }
}
