package com.facochico.edu.domain.quiz;

import com.facochico.edu.domain.quiz.dto.QuizScore;
import com.facochico.edu.domain.user.UserEntity;

import java.time.Instant;

import jakarta.persistence.CheckConstraint;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.ForeignKey;
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

@Entity
@Table(
    name = "quiz_submissions",
    check = @CheckConstraint(name = "ck_quiz_submissions_score", constraint = "max_score > 0 and score between 0 and max_score"),
    indexes = {
        @Index(name = "ix_quiz_submissions_quiz", columnList = "quiz_id"),
        @Index(name = "ix_quiz_submissions_student", columnList = "student_id")
    }
)
@Getter
@NoArgsConstructor(access = PROTECTED)
public class QuizSubmissionEntity {
    public static final String QUIZ_FOREIGN_KEY = "fk_quiz_submissions_quiz";

    @Id
    @GeneratedValue(strategy = SEQUENCE)
    private Long id;

    @ManyToOne(fetch = LAZY, optional = false)
    @JoinColumn(name = "quiz_id", nullable = false, foreignKey = @ForeignKey(name = QUIZ_FOREIGN_KEY))
    private QuizEntity quiz;

    @ManyToOne(fetch = LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private UserEntity student;

    @Column(nullable = false)
    private int score;

    @Column(nullable = false)
    private int maxScore;

    @Column(nullable = false)
    private Instant takenAt;

    public QuizSubmissionEntity(QuizEntity quiz, UserEntity student, QuizScore result, Instant takenAt) {
        this.quiz = quiz;
        this.student = student;
        score = result.score();
        maxScore = result.maxScore();
        this.takenAt = takenAt;
    }
}
