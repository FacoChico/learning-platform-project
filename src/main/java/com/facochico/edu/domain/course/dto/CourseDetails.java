package com.facochico.edu.domain.course.dto;

import com.facochico.edu.util.course.CurriculumValidationUtil;

import java.util.Set;

public record CourseDetails(
    String title,
    String description,
    long categoryId,
    long teacherId,
    Set<Long> tagIds
) {

    public CourseDetails {
        title = CurriculumValidationUtil.requireLabel(title, "Course title");
        CurriculumValidationUtil.requireId(categoryId, "Category");
        CurriculumValidationUtil.requireId(teacherId, "Teacher");
        tagIds = CurriculumValidationUtil.copyTagIds(tagIds);
    }
}
