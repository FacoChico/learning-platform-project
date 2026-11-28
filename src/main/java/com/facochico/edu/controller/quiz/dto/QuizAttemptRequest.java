package com.facochico.edu.controller.quiz.dto;

import java.util.Map;

import jakarta.validation.constraints.NotEmpty;

public record QuizAttemptRequest(@NotEmpty Map<Long, Long> answers) {
}
