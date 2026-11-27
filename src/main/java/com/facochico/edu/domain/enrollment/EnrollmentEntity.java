package com.facochico.edu.domain.enrollment;

import com.facochico.edu.domain.course.CourseEntity;
import com.facochico.edu.domain.user.UserEntity;
import com.facochico.edu.util.user.UserValidationUtil;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
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

import static com.facochico.edu.domain.enrollment.EnrollmentStatus.ACTIVE;
import static com.facochico.edu.domain.enrollment.EnrollmentStatus.COMPLETED;
import static jakarta.persistence.EnumType.STRING;
import static jakarta.persistence.FetchType.LAZY;
import static jakarta.persistence.GenerationType.SEQUENCE;
import static lombok.AccessLevel.PROTECTED;

@Entity
@Table(
    name = "enrollments",
    uniqueConstraints = @UniqueConstraint(name = EnrollmentEntity.STUDENT_COURSE_UNIQUE, columnNames = {"student_id", "course_id"}),
    indexes = @Index(name = "ix_enrollments_course", columnList = "course_id")
)
@Getter
@NoArgsConstructor(access = PROTECTED)
public class EnrollmentEntity {
    public static final String STUDENT_COURSE_UNIQUE = "uk_enrollments_student_course";
    public static final String COURSE_FOREIGN_KEY = "fk_enrollments_course";

    @Id
    @GeneratedValue(strategy = SEQUENCE)
    private Long id;

    @ManyToOne(fetch = LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private UserEntity student;

    @ManyToOne(fetch = LAZY, optional = false)
    @JoinColumn(name = "course_id", nullable = false, foreignKey = @ForeignKey(name = COURSE_FOREIGN_KEY))
    private CourseEntity course;

    @Column(nullable = false)
    private Instant enrolledAt;

    @Enumerated(STRING)
    @Column(nullable = false)
    private EnrollmentStatus status;

    public EnrollmentEntity(CourseEntity course, UserEntity student, Instant enrolledAt) {
        UserValidationUtil.requireStudent(student.getRole());

        this.course = course;
        this.student = student;
        this.enrolledAt = enrolledAt;
        status = ACTIVE;
    }

    public void complete() {
        status = COMPLETED;
    }
}
