package com.facochico.edu.repository.course;

import com.facochico.edu.domain.course.CourseEntity;
import com.facochico.edu.domain.course.CourseReviewEntity;
import com.facochico.edu.domain.course.dto.CourseReviewDetails;
import com.facochico.edu.domain.course.dto.CourseReviewView;
import com.facochico.edu.domain.course.exception.CurriculumItemNotFoundException;
import com.facochico.edu.domain.enrollment.EnrollmentEntity;
import com.facochico.edu.domain.enrollment.dto.EnrolledCourse;
import com.facochico.edu.domain.enrollment.dto.EnrolledStudent;
import com.facochico.edu.domain.enrollment.dto.EnrollmentView;
import com.facochico.edu.domain.enrollment.exception.DuplicateEnrollmentException;
import com.facochico.edu.domain.user.UserEntity;
import com.facochico.edu.domain.user.exception.UserNotFoundException;

import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import jakarta.persistence.EntityManager;

import lombok.RequiredArgsConstructor;

import static com.facochico.edu.domain.enrollment.EnrollmentEntity.STUDENT_COURSE_UNIQUE;
import static lombok.AccessLevel.PACKAGE;

@Repository
@Transactional
@RequiredArgsConstructor(access = PACKAGE)
class JpaCourseParticipationRepository implements EnrollmentRepository, CourseReviewRepository {
    private final SpringDataEnrollmentRepository enrollments;
    private final SpringDataCourseReviewRepository reviews;
    private final EntityManager entityManager;

    @Override
    public EnrollmentView insertEnrollment(long courseId, long studentId, Instant enrolledAt) {
        final var course = requireCourse(courseId);
        final var student = requireUser(studentId);
        final var enrollment = new EnrollmentEntity(course, student, enrolledAt);

        try {
            return toEnrollmentView(enrollments.saveAndFlush(enrollment));
        } catch (DataIntegrityViolationException exception) {
            if (exception.getCause() instanceof ConstraintViolationException violation
                && STUDENT_COURSE_UNIQUE.equals(violation.getConstraintName())) {
                throw new DuplicateEnrollmentException(courseId, studentId, exception);
            }
            throw exception;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<EnrollmentView> findEnrollment(long courseId, long studentId) {
        return enrollments.findByCourseIdAndStudentId(courseId, studentId)
            .map(JpaCourseParticipationRepository::toEnrollmentView);
    }

    @Override
    public Optional<EnrollmentView> completeEnrollment(long courseId, long studentId) {
        return enrollments.findByCourseIdAndStudentId(courseId, studentId)
            .map(enrollment -> {
                enrollment.complete();
                return toEnrollmentView(enrollment);
            });
    }

    @Override
    public boolean deleteEnrollment(long courseId, long studentId) {
        return enrollments.deleteEnrollment(courseId, studentId) > 0;
    }

    @Override
    @Transactional(readOnly = true)
    public List<EnrolledStudent> listStudents(long courseId, int page, int size) {
        requireCourse(courseId);
        return enrollments.findStudents(courseId, PageRequest.of(page, size));
    }

    @Override
    @Transactional(readOnly = true)
    public List<EnrolledCourse> listCourses(long studentId, int page, int size) {
        requireUser(studentId);
        return enrollments.findCourses(studentId, PageRequest.of(page, size));
    }

    @Override
    public CourseReviewView insertReview(long courseId, long studentId, CourseReviewDetails details, Instant createdAt) {
        final var course = requireCourse(courseId);
        final var student = requireUser(studentId);
        final var review = reviews.save(new CourseReviewEntity(course, student, createdAt, details));
        return toReviewView(review);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<CourseReviewView> findReviewById(long id) {
        return reviews.findById(id)
            .map(JpaCourseParticipationRepository::toReviewView);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CourseReviewView> listReviews(long courseId, int page, int size) {
        requireCourse(courseId);
        return reviews.findByCourseIdOrderById(courseId, PageRequest.of(page, size)).stream()
            .map(JpaCourseParticipationRepository::toReviewView)
            .toList();
    }

    @Override
    public Optional<CourseReviewView> updateReview(long id, CourseReviewDetails details) {
        return reviews.findById(id)
            .map(review -> {
                review.update(details);
                return toReviewView(review);
            });
    }

    @Override
    public boolean deleteReview(long id) {
        final var review = reviews.findById(id);
        if (review.isEmpty()) {
            return false;
        }

        reviews.delete(review.get());
        return true;
    }

    private CourseEntity requireCourse(long id) {
        final var course = entityManager.find(CourseEntity.class, id);
        if (course == null) {
            throw new CurriculumItemNotFoundException("Course", id);
        }
        return course;
    }

    private UserEntity requireUser(long id) {
        final var user = entityManager.find(UserEntity.class, id);
        if (user == null) {
            throw new UserNotFoundException(id);
        }
        return user;
    }

    private static EnrollmentView toEnrollmentView(EnrollmentEntity enrollment) {
        return new EnrollmentView(
            enrollment.getId(), enrollment.getCourse().getId(), enrollment.getStudent().getId(),
            enrollment.getEnrolledAt(), enrollment.getStatus()
        );
    }

    private static CourseReviewView toReviewView(CourseReviewEntity review) {
        final var details = new CourseReviewDetails(review.getRating(), review.getComment());
        return new CourseReviewView(
            review.getId(), review.getCourse().getId(), review.getStudent().getId(), review.getCreatedAt(), details
        );
    }
}
