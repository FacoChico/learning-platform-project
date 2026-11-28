package com.facochico.edu.controller.homework.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record GradeRequest(@NotNull @PositiveOrZero Integer score, String feedback) {
}
