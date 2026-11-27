package com.facochico.edu.domain.quiz.dto;

import java.util.Set;

public record QuizQuestionKey(long questionId, Set<Long> optionIds, long correctOptionId) {
}
