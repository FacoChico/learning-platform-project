package com.facochico.edu.domain.homework.dto;

import com.facochico.edu.domain.homework.SubmissionStatus;

import java.time.Instant;

public record SubmissionView(
    long id,
    long assignmentId,
    long studentId,
    Instant submittedAt,
    String content,
    SubmissionStatus status,
    Integer score,
    String feedback
) {
}
