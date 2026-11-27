package com.facochico.edu.domain.course.dto;

import com.facochico.edu.util.course.CurriculumValidationUtil;

public record LessonDetails(String title, String content, String videoUrl, int orderIndex) {

    public LessonDetails {
        title = CurriculumValidationUtil.requireLabel(title, "Lesson title");
        CurriculumValidationUtil.requirePosition(orderIndex);
    }
}
