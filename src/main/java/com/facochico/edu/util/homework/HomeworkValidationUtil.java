package com.facochico.edu.util.homework;

import com.facochico.edu.domain.homework.exception.InvalidScoreException;
import com.facochico.edu.domain.homework.exception.SubmissionDeadlinePassedException;

import java.time.Instant;

import lombok.experimental.UtilityClass;

@UtilityClass
public class HomeworkValidationUtil {

    public void requireAssignmentDetails(Instant dueAt, int maxScore) {
        if (dueAt == null) {
            throw new IllegalArgumentException("Assignment deadline is required");
        }
        if (maxScore < 1) {
            throw new IllegalArgumentException("Assignment max score must be positive");
        }
    }

    public void requireMaxScoreAtLeastHighestGrade(int maxScore, int highestScore) {
        if (maxScore < highestScore) {
            throw new InvalidScoreException("Assignment max score cannot be below an existing grade");
        }
    }

    public void requireDeadlineMet(long assignmentId, Instant submittedAt, Instant dueAt) {
        if (submittedAt.isAfter(dueAt)) {
            throw new SubmissionDeadlinePassedException(assignmentId);
        }
    }

    public void requireSubmissionContent(String content) {
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("Submission content is required");
        }
    }

    public void requireScoreInRange(int score, int maxScore) {
        if (score < 0 || score > maxScore) {
            throw new InvalidScoreException("Score must be between 0 and " + maxScore);
        }
    }
}
