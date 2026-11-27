package com.facochico.edu.domain.quiz.exception;

public final class QuizAccessDeniedException extends RuntimeException {

    public QuizAccessDeniedException(String message) {
        super(message);
    }
}
