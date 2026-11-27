package com.facochico.edu.repository.course;

import com.facochico.edu.domain.enrollment.dto.EnrolledCourse;
import com.facochico.edu.domain.enrollment.dto.EnrolledStudent;
import com.facochico.edu.domain.enrollment.dto.EnrollmentView;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface EnrollmentRepository {

    EnrollmentView insertEnrollment(long courseId, long studentId, Instant enrolledAt);

    Optional<EnrollmentView> findEnrollment(long courseId, long studentId);

    Optional<EnrollmentView> completeEnrollment(long courseId, long studentId);

    boolean deleteEnrollment(long courseId, long studentId);

    List<EnrolledStudent> listStudents(long courseId, int page, int size);

    List<EnrolledCourse> listCourses(long studentId, int page, int size);
}
