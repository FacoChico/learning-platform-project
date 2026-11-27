package com.facochico.edu.domain.homework.dto;

import com.facochico.edu.util.course.CurriculumValidationUtil;
import com.facochico.edu.util.homework.HomeworkValidationUtil;

import java.time.Instant;

import static java.time.temporal.ChronoUnit.MICROS;

public record AssignmentDetails(String title, String description, Instant dueAt, int maxScore) {

    public AssignmentDetails {
        title = CurriculumValidationUtil.requireLabel(title, "Assignment title");
        HomeworkValidationUtil.requireAssignmentDetails(dueAt, maxScore);
        dueAt = dueAt.truncatedTo(MICROS);
    }
}
