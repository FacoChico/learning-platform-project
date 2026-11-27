package com.facochico.edu.util.quiz;

import com.facochico.edu.domain.quiz.dto.QuizQuestionKey;
import com.facochico.edu.domain.quiz.dto.QuizScore;
import com.facochico.edu.domain.quiz.exception.InvalidQuizAnswersException;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

class QuizScoringUtilTest {
    private static final List<QuizQuestionKey> QUESTIONS = List.of(
        new QuizQuestionKey(1L, Set.of(11L, 12L), 11L),
        new QuizQuestionKey(2L, Set.of(21L, 22L), 22L)
    );

    @Test
    void shouldAwardOnePointWhenOneOfTwoAnswersIsCorrect() {
        final var answers = Map.of(1L, 11L, 2L, 21L);

        final var score = QuizScoringUtil.score(QUESTIONS, answers);

        assertThat(score).isEqualTo(new QuizScore(1, 2));
    }

    @Test
    void shouldRejectAttemptWhenQuestionIsUnanswered() {
        final var answers = Map.of(1L, 11L);

        final var failure = catchThrowable(() -> QuizScoringUtil.score(QUESTIONS, answers));

        assertThat(failure).isInstanceOf(InvalidQuizAnswersException.class)
            .hasMessage("Select one answer option for every question in the quiz");
    }

    @Test
    void shouldRejectAttemptWhenSelectedOptionBelongsToAnotherQuestion() {
        final var answers = Map.of(1L, 21L, 2L, 22L);

        final var failure = catchThrowable(() -> QuizScoringUtil.score(QUESTIONS, answers));

        assertThat(failure).isInstanceOf(InvalidQuizAnswersException.class)
            .hasMessage("Selected answer option does not belong to question 1");
    }

    @Test
    void shouldRejectAttemptWhenAnswerRefersToUnknownQuestion() {
        final var answers = Map.of(1L, 11L, 3L, 22L);

        final var failure = catchThrowable(() -> QuizScoringUtil.score(QUESTIONS, answers));

        assertThat(failure).isInstanceOf(InvalidQuizAnswersException.class);
    }
}
