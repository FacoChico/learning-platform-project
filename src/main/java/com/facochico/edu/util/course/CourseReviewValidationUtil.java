package com.facochico.edu.util.course;

import lombok.experimental.UtilityClass;

@UtilityClass
public class CourseReviewValidationUtil {

    public void requireRating(int rating) {
        if (rating < 1 || rating > 5) {
            throw new IllegalArgumentException("Course review rating must be between 1 and 5");
        }
    }
}
