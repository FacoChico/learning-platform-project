package com.facochico.edu.domain.quiz.dto;

import com.facochico.edu.util.course.CurriculumValidationUtil;
import com.facochico.edu.util.quiz.QuizValidationUtil;

import java.util.List;

public record QuizDetails(String title, List<QuestionDetails> questions) {

    public QuizDetails {
        title = CurriculumValidationUtil.requireLabel(title, "Quiz title");
        questions = QuizValidationUtil.requireItems(questions, 1, "A quiz must have at least one question");
    }

    public record QuestionDetails(String text, List<OptionDetails> options) {

        public QuestionDetails {
            QuizValidationUtil.requireText(text, "Question text");
            options = QuizValidationUtil.requireItems(options, 2, "A question must have at least two answer options");
            QuizValidationUtil.requireOneCorrectOption(options.stream().filter(OptionDetails::correct).count());
        }
    }

    public record OptionDetails(String text, boolean correct) {

        public OptionDetails {
            QuizValidationUtil.requireText(text, "Answer option text");
        }
    }
}
