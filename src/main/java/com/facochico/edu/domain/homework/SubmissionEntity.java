package com.facochico.edu.domain.homework;

import com.facochico.edu.domain.user.UserEntity;
import com.facochico.edu.util.homework.HomeworkValidationUtil;

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
import jakarta.persistence.UniqueConstraint;

import lombok.Getter;
import lombok.NoArgsConstructor;

import static com.facochico.edu.domain.homework.SubmissionStatus.GRADED;
import static com.facochico.edu.domain.homework.SubmissionStatus.SUBMITTED;
import static jakarta.persistence.FetchType.LAZY;
import static jakarta.persistence.GenerationType.SEQUENCE;
import static lombok.AccessLevel.PROTECTED;

@Entity
@Table(
    name = "submissions",
    check = @CheckConstraint(name = "ck_submissions_score", constraint = "score >= 0"),
    uniqueConstraints = @UniqueConstraint(
        name = SubmissionEntity.ASSIGNMENT_STUDENT_UNIQUE, columnNames = {"assignment_id", "student_id"}
    ),
    indexes = @Index(name = "ix_submissions_student", columnList = "student_id")
)
@Getter
@NoArgsConstructor(access = PROTECTED)
public class SubmissionEntity {
    public static final String ASSIGNMENT_STUDENT_UNIQUE = "uk_submissions_assignment_student";
    public static final String ASSIGNMENT_FOREIGN_KEY = "fk_submissions_assignment";

    @Id
    @GeneratedValue(strategy = SEQUENCE)
    private Long id;

    @ManyToOne(fetch = LAZY, optional = false)
    @JoinColumn(name = "assignment_id", nullable = false, foreignKey = @ForeignKey(name = ASSIGNMENT_FOREIGN_KEY))
    private AssignmentEntity assignment;

    @ManyToOne(fetch = LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private UserEntity student;

    @Column(nullable = false)
    private Instant submittedAt;

    @Column(nullable = false, columnDefinition = "text")
    private String content;

    private Integer score;

    @Column(columnDefinition = "text")
    private String feedback;

    public SubmissionEntity(AssignmentEntity assignment, UserEntity student, Instant submittedAt, String content) {
        HomeworkValidationUtil.requireDeadlineMet(assignment.getId(), submittedAt, assignment.getDueAt());

        this.assignment = assignment;
        this.student = student;
        this.submittedAt = submittedAt;
        this.content = content;
    }

    public void grade(int score, String feedback) {
        HomeworkValidationUtil.requireScoreInRange(score, assignment.getMaxScore());

        this.score = score;
        this.feedback = feedback;
    }

    public SubmissionStatus getStatus() {
        return score == null ? SUBMITTED : GRADED;
    }
}
