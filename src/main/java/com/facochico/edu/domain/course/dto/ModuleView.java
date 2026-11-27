package com.facochico.edu.domain.course.dto;

import java.util.List;

public record ModuleView(long id, long courseId, ModuleDetails details, List<LessonView> lessons) {
}
