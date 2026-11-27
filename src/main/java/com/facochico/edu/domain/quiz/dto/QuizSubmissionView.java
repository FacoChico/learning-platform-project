package com.facochico.edu.domain.quiz.dto;

import java.time.Instant;

public record QuizSubmissionView(long id, long quizId, long studentId, int score, int maxScore, Instant takenAt) {
}
