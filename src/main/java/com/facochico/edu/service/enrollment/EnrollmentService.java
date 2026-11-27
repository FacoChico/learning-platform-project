package com.facochico.edu.service.enrollment;

import com.facochico.edu.domain.enrollment.dto.EnrolledCourse;
import com.facochico.edu.domain.enrollment.dto.EnrolledStudent;
import com.facochico.edu.domain.enrollment.dto.EnrollmentView;
import com.facochico.edu.domain.enrollment.exception.EnrollmentNotFoundException;
import com.facochico.edu.repository.course.EnrollmentRepository;
import com.facochico.edu.util.PaginationValidationUtil;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

import lombok.RequiredArgsConstructor;

import static java.time.temporal.ChronoUnit.MICROS;

@RequiredArgsConstructor
public class EnrollmentService {
    private final EnrollmentRepository enrollments;
    private final Clock clock;

    public EnrollmentView enroll(long courseId, long studentId) {
        final var enrolledAt = Instant.now(clock).truncatedTo(MICROS);
        return enrollments.insertEnrollment(courseId, studentId, enrolledAt);
    }

    public EnrollmentView findEnrollment(long courseId, long studentId) {
        return enrollments.findEnrollment(courseId, studentId)
            .orElseThrow(() -> new EnrollmentNotFoundException(courseId, studentId));
    }

    public EnrollmentView completeEnrollment(long courseId, long studentId) {
        return enrollments.completeEnrollment(courseId, studentId)
            .orElseThrow(() -> new EnrollmentNotFoundException(courseId, studentId));
    }

    public void withdraw(long courseId, long studentId) {
        if (!enrollments.deleteEnrollment(courseId, studentId)) {
            throw new EnrollmentNotFoundException(courseId, studentId);
        }
    }

    public List<EnrolledStudent> listStudents(long courseId, int page, int size) {
        PaginationValidationUtil.requirePage(page, size);
        return enrollments.listStudents(courseId, page, size);
    }

    public List<EnrolledCourse> listCourses(long studentId, int page, int size) {
        PaginationValidationUtil.requirePage(page, size);
        return enrollments.listCourses(studentId, page, size);
    }
}
