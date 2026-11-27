package com.facochico.edu.domain.course.dto;

import com.facochico.edu.util.course.CourseReviewValidationUtil;

public record CourseReviewDetails(int rating, String comment) {

    public CourseReviewDetails {
        CourseReviewValidationUtil.requireRating(rating);
    }
}
