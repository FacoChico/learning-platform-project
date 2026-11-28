package com.facochico.edu.controller.homework.dto;

import jakarta.validation.constraints.NotBlank;

public record SubmissionRequest(@NotBlank String content) {
}
