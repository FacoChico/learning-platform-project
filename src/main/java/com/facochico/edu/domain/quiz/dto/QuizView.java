package com.facochico.edu.domain.quiz.dto;

import java.util.List;

public record QuizView(long id, long moduleId, String title, List<QuestionView> questions) {

    public record QuestionView(long id, String text, List<OptionView> options) {
    }

    public record OptionView(long id, String text) {
    }
}
