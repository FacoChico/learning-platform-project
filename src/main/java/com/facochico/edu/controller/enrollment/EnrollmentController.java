package com.facochico.edu.controller.enrollment;

import com.facochico.edu.controller.dto.PageQuery;
import com.facochico.edu.domain.enrollment.dto.EnrolledCourse;
import com.facochico.edu.domain.enrollment.dto.EnrolledStudent;
import com.facochico.edu.domain.enrollment.dto.EnrollmentView;
import com.facochico.edu.service.enrollment.EnrollmentService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;

import lombok.RequiredArgsConstructor;

import static org.springframework.http.HttpStatus.NO_CONTENT;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
class EnrollmentController {
    private final EnrollmentService enrollments;

    @PostMapping("/courses/{courseId}/enrollments")
    ResponseEntity<EnrollmentView> enroll(
        @PathVariable @Positive long courseId,
        @RequestHeader("X-User-Id") @Positive long studentId
    ) {
        final var enrollment = enrollments.enroll(courseId, studentId);
        final var location = URI.create("/api/v1/courses/%d/enrollments/%d".formatted(courseId, studentId));
        return ResponseEntity.created(location).body(enrollment);
    }

    @GetMapping("/courses/{courseId}/enrollments/{studentId}")
    EnrollmentView find(@PathVariable @Positive long courseId, @PathVariable @Positive long studentId) {
        return enrollments.findEnrollment(courseId, studentId);
    }

    @PutMapping("/courses/{courseId}/enrollments/{studentId}/completion")
    EnrollmentView complete(@PathVariable @Positive long courseId, @PathVariable @Positive long studentId) {
        return enrollments.completeEnrollment(courseId, studentId);
    }

    @DeleteMapping("/courses/{courseId}/enrollments/{studentId}")
    @ResponseStatus(NO_CONTENT)
    void withdraw(@PathVariable @Positive long courseId, @PathVariable @Positive long studentId) {
        enrollments.withdraw(courseId, studentId);
    }

    @GetMapping("/courses/{courseId}/students")
    List<EnrolledStudent> listStudents(@PathVariable @Positive long courseId, @ModelAttribute @Valid PageQuery page) {
        return enrollments.listStudents(courseId, page.page(), page.size());
    }

    @GetMapping("/users/{studentId}/courses")
    List<EnrolledCourse> listCourses(@PathVariable @Positive long studentId, @ModelAttribute @Valid PageQuery page) {
        return enrollments.listCourses(studentId, page.page(), page.size());
    }
}
