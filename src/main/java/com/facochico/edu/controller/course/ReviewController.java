package com.facochico.edu.controller.course;

import com.facochico.edu.controller.dto.PageQuery;
import com.facochico.edu.domain.course.dto.CourseReviewDetails;
import com.facochico.edu.domain.course.dto.CourseReviewView;
import com.facochico.edu.service.course.CourseReviewService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
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
class ReviewController {
    private final CourseReviewService reviews;

    @PostMapping("/courses/{courseId}/reviews")
    ResponseEntity<CourseReviewView> create(
        @PathVariable @Positive long courseId,
        @RequestHeader("X-User-Id") @Positive long studentId,
        @RequestBody CourseReviewDetails details
    ) {
        final var review = reviews.createReview(courseId, studentId, details);
        return ResponseEntity.created(URI.create("/api/v1/reviews/" + review.id())).body(review);
    }

    @GetMapping("/courses/{courseId}/reviews")
    List<CourseReviewView> list(@PathVariable @Positive long courseId, @ModelAttribute @Valid PageQuery page) {
        return reviews.listReviews(courseId, page.page(), page.size());
    }

    @GetMapping("/reviews/{id}")
    CourseReviewView find(@PathVariable @Positive long id) {
        return reviews.findReviewById(id);
    }

    @PutMapping("/reviews/{id}")
    CourseReviewView update(
        @PathVariable @Positive long id,
        @RequestHeader("X-User-Id") @Positive long authorId,
        @RequestBody CourseReviewDetails details
    ) {
        return reviews.updateReview(id, authorId, details);
    }

    @DeleteMapping("/reviews/{id}")
    @ResponseStatus(NO_CONTENT)
    void delete(@PathVariable @Positive long id, @RequestHeader("X-User-Id") @Positive long authorId) {
        reviews.deleteReview(id, authorId);
    }
}
