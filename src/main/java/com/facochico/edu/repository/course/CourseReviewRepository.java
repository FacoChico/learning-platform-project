package com.facochico.edu.repository.course;

import com.facochico.edu.domain.course.dto.CourseReviewDetails;
import com.facochico.edu.domain.course.dto.CourseReviewView;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface CourseReviewRepository {

    CourseReviewView insertReview(long courseId, long studentId, CourseReviewDetails details, Instant createdAt);

    Optional<CourseReviewView> findReviewById(long id);

    List<CourseReviewView> listReviews(long courseId, int page, int size);

    Optional<CourseReviewView> updateReview(long id, CourseReviewDetails details);

    boolean deleteReview(long id);
}
