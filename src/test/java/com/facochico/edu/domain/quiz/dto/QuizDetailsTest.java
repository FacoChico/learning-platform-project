package com.facochico.edu.domain.quiz.dto;

import com.facochico.edu.domain.quiz.dto.QuizDetails.OptionDetails;
import com.facochico.edu.domain.quiz.dto.QuizDetails.QuestionDetails;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

class QuizDetailsTest {

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void shouldRejectQuestionWhenCorrectOptionCountDiffersFromOne(boolean correct) {
        final var options = List.of(new OptionDetails("Первый вариант", correct), new OptionDetails("Второй вариант", correct));

        final var failure = catchThrowable(() -> new QuestionDetails("Как работает журнал транзакций?", options));

        assertThat(failure).isInstanceOf(IllegalArgumentException.class)
            .hasMessage("A question must have exactly one correct answer option");
    }

    @Test
    void shouldRejectQuestionWhenOnlyOneOptionIsProvided() {
        final var options = List.of(new OptionDetails("Первичный ключ", true));

        final var failure = catchThrowable(() -> new QuestionDetails("Что однозначно определяет строку?", options));

        assertThat(failure).isInstanceOf(IllegalArgumentException.class)
            .hasMessage("A question must have at least two answer options");
    }

    @Test
    void shouldRejectQuizWhenDefinitionContainsNoQuestions() {
        final var title = "Проверка знаний по SQL";

        final var failure = catchThrowable(() -> new QuizDetails(title, List.of()));

        assertThat(failure).isInstanceOf(IllegalArgumentException.class)
            .hasMessage("A quiz must have at least one question");
    }
}
