package com.facochico.edu.domain.course;

import com.facochico.edu.domain.course.dto.CourseReviewDetails;
import com.facochico.edu.domain.user.UserEntity;
import com.facochico.edu.util.user.UserValidationUtil;

import org.hibernate.annotations.OnDelete;

import java.time.Instant;

import jakarta.persistence.CheckConstraint;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

import static jakarta.persistence.FetchType.LAZY;
import static jakarta.persistence.GenerationType.SEQUENCE;
import static lombok.AccessLevel.PROTECTED;
import static org.hibernate.annotations.OnDeleteAction.CASCADE;

@Entity
@Table(
    name = "course_reviews",
    check = @CheckConstraint(name = "ck_course_reviews_rating", constraint = "rating between 1 and 5"),
    indexes = {
        @Index(name = "ix_course_reviews_course", columnList = "course_id"),
        @Index(name = "ix_course_reviews_student", columnList = "student_id")
    }
)
@Getter
@NoArgsConstructor(access = PROTECTED)
public class CourseReviewEntity {
    @Id
    @GeneratedValue(strategy = SEQUENCE)
    private Long id;

    @ManyToOne(fetch = LAZY, optional = false)
    @JoinColumn(name = "course_id", nullable = false)
    @OnDelete(action = CASCADE)
    private CourseEntity course;

    @ManyToOne(fetch = LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private UserEntity student;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private int rating;

    @Column(columnDefinition = "text")
    private String comment;

    public CourseReviewEntity(CourseEntity course, UserEntity student, Instant createdAt, CourseReviewDetails details) {
        UserValidationUtil.requireStudent(student.getRole());

        this.course = course;
        this.student = student;
        this.createdAt = createdAt;
        update(details);
    }

    public void update(CourseReviewDetails details) {
        rating = details.rating();
        comment = details.comment();
    }
}
