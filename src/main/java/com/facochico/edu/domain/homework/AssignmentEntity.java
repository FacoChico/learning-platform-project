package com.facochico.edu.domain.homework;

import com.facochico.edu.domain.course.LessonEntity;
import com.facochico.edu.domain.homework.dto.AssignmentDetails;
import com.facochico.edu.util.homework.HomeworkValidationUtil;

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

import static org.hibernate.annotations.OnDeleteAction.CASCADE;
import static jakarta.persistence.FetchType.LAZY;
import static jakarta.persistence.GenerationType.SEQUENCE;
import static lombok.AccessLevel.PROTECTED;

@Entity
@Table(
    name = "assignments",
    check = @CheckConstraint(name = "ck_assignments_max_score", constraint = "max_score > 0"),
    indexes = @Index(name = "ix_assignments_lesson", columnList = "lesson_id")
)
@Getter
@NoArgsConstructor(access = PROTECTED)
public class AssignmentEntity {
    @Id
    @GeneratedValue(strategy = SEQUENCE)
    private Long id;

    @ManyToOne(fetch = LAZY, optional = false)
    @JoinColumn(name = "lesson_id", nullable = false)
    @OnDelete(action = CASCADE)
    private LessonEntity lesson;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "text")
    private String description;

    @Column(nullable = false)
    private Instant dueAt;

    @Column(nullable = false)
    private int maxScore;

    public AssignmentEntity(LessonEntity lesson, AssignmentDetails details) {
        this.lesson = lesson;
        update(details, 0);
    }

    public void update(AssignmentDetails details, int highestScore) {
        HomeworkValidationUtil.requireMaxScoreAtLeastHighestGrade(details.maxScore(), highestScore);

        title = details.title();
        description = details.description();
        dueAt = details.dueAt();
        maxScore = details.maxScore();
    }
}
