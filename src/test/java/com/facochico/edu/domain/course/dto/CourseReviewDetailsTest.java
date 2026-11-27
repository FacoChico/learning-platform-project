package com.facochico.edu.domain.course.dto;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

class CourseReviewDetailsTest {

    @ParameterizedTest
    @ValueSource(ints = {0, 6})
    void shouldRejectRatingWhenItFallsOutsideTheReviewScale(int rating) {
        final var comment = "Разбор задач помог разобраться в теме";

        final var failure = catchThrowable(() -> new CourseReviewDetails(rating, comment));

        assertThat(failure).isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Course review rating must be between 1 and 5");
    }
}
