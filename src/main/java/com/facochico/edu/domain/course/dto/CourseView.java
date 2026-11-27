package com.facochico.edu.domain.course.dto;

import java.util.List;

public record CourseView(
    long id,
    String title,
    String description,
    long teacherId,
    CatalogEntry category,
    List<CatalogEntry> tags,
    List<ModuleView> modules
) {
}
