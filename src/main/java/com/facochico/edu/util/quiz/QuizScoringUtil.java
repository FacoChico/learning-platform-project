package com.facochico.edu.util.quiz;

import com.facochico.edu.domain.quiz.dto.QuizQuestionKey;
import com.facochico.edu.domain.quiz.dto.QuizScore;
import com.facochico.edu.domain.quiz.exception.InvalidQuizAnswersException;

import java.util.List;
import java.util.Map;

import lombok.experimental.UtilityClass;

@UtilityClass
public class QuizScoringUtil {

    public QuizScore score(List<QuizQuestionKey> questions, Map<Long, Long> answers) {
        if (answers.size() != questions.size()) {
            throw new InvalidQuizAnswersException("Select one answer option for every question in the quiz");
        }

        var correct = 0;
        for (final var question : questions) {
            final var selected = answers.get(question.questionId());
            if (selected == null || !question.optionIds().contains(selected)) {
                throw new InvalidQuizAnswersException(
                    "Selected answer option does not belong to question %d".formatted(question.questionId())
                );
            }
            if (selected == question.correctOptionId()) {
                correct++;
            }
        }

        return new QuizScore(correct, questions.size());
    }
}
