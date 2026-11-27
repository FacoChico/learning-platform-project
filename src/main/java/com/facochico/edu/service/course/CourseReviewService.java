package com.facochico.edu.service.course;

import com.facochico.edu.domain.course.dto.CourseReviewDetails;
import com.facochico.edu.domain.course.dto.CourseReviewView;
import com.facochico.edu.domain.course.exception.CourseReviewNotFoundException;
import com.facochico.edu.repository.course.CourseReviewRepository;
import com.facochico.edu.util.PaginationValidationUtil;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

import lombok.RequiredArgsConstructor;

import static java.time.temporal.ChronoUnit.MICROS;

@RequiredArgsConstructor
public class CourseReviewService {
    private final CourseReviewRepository reviews;
    private final Clock clock;

    public CourseReviewView createReview(long courseId, long studentId, CourseReviewDetails details) {
        final var createdAt = Instant.now(clock).truncatedTo(MICROS);
        return reviews.insertReview(courseId, studentId, details, createdAt);
    }

    public CourseReviewView findReviewById(long id) {
        return reviews.findReviewById(id)
            .orElseThrow(() -> new CourseReviewNotFoundException(id));
    }

    public List<CourseReviewView> listReviews(long courseId, int page, int size) {
        PaginationValidationUtil.requirePage(page, size);
        return reviews.listReviews(courseId, page, size);
    }

    public CourseReviewView updateReview(long id, CourseReviewDetails details) {
        return reviews.updateReview(id, details)
            .orElseThrow(() -> new CourseReviewNotFoundException(id));
    }

    public void deleteReview(long id) {
        if (!reviews.deleteReview(id)) {
            throw new CourseReviewNotFoundException(id);
        }
    }
}
