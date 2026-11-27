package com.facochico.edu.domain.enrollment.dto;

import com.facochico.edu.domain.enrollment.EnrollmentStatus;

import java.time.Instant;

public record EnrollmentView(long id, long courseId, long studentId, Instant enrolledAt, EnrollmentStatus status) {
}
