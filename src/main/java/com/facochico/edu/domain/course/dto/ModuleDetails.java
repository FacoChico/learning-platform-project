package com.facochico.edu.domain.course.dto;

import com.facochico.edu.util.course.CurriculumValidationUtil;

public record ModuleDetails(String title, String description, int orderIndex) {

    public ModuleDetails {
        title = CurriculumValidationUtil.requireLabel(title, "Module title");
        CurriculumValidationUtil.requirePosition(orderIndex);
    }
}
