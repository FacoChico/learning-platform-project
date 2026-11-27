package com.facochico.edu.util.quiz;

import com.facochico.edu.domain.quiz.exception.InvalidQuizAnswersException;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import lombok.experimental.UtilityClass;

@UtilityClass
public class QuizValidationUtil {

    public void requireText(String text, String label) {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException(label + " is required");
        }
    }

    public <T> List<T> requireItems(List<T> items, int minimum, String message) {
        if (items == null || items.size() < minimum || items.stream().anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException(message);
        }
        return List.copyOf(items);
    }

    public void requireOneCorrectOption(long correctOptions) {
        if (correctOptions != 1) {
            throw new IllegalArgumentException("A question must have exactly one correct answer option");
        }
    }

    public Map<Long, Long> copyAnswers(Map<Long, Long> answers) {
        if (answers == null) {
            throw new InvalidQuizAnswersException("Quiz answers are required");
        }

        answers.forEach((questionId, optionId) -> {
            final var validQuestion = questionId != null && questionId > 0;
            final var validOption = optionId != null && optionId > 0;
            if (!validQuestion || !validOption) {
                throw new InvalidQuizAnswersException("Question and answer option IDs must be positive");
            }
        });
        return Map.copyOf(answers);
    }
}
