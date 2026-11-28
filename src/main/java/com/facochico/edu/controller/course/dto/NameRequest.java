package com.facochico.edu.controller.course.dto;

import jakarta.validation.constraints.NotBlank;

public record NameRequest(@NotBlank String name) {
}
