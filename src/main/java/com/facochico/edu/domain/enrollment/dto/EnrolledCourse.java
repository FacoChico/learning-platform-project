package com.facochico.edu.domain.enrollment.dto;

import com.facochico.edu.domain.enrollment.EnrollmentStatus;

public record EnrolledCourse(Long id, String title, EnrollmentStatus status) {
}
