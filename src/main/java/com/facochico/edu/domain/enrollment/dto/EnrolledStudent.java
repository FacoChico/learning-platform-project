package com.facochico.edu.domain.enrollment.dto;

import com.facochico.edu.domain.enrollment.EnrollmentStatus;

public record EnrolledStudent(Long id, String name, EnrollmentStatus status) {
}
