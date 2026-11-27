package com.facochico.edu.domain.quiz.exception;

public final class QuizInUseException extends RuntimeException {

    public QuizInUseException(String resource, long id, Throwable cause) {
        super("%s %d has quiz results".formatted(resource, id), cause);
    }
}
