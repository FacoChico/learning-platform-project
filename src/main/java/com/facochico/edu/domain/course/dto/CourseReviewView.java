package com.facochico.edu.domain.course.dto;

import java.time.Instant;

public record CourseReviewView(long id, long courseId, long studentId, Instant createdAt, CourseReviewDetails details) {
}
