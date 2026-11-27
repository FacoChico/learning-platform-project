package com.facochico.edu.domain.course;

import com.facochico.edu.domain.course.dto.LessonDetails;
import com.facochico.edu.domain.course.dto.ModuleDetails;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

import lombok.Getter;
import lombok.NoArgsConstructor;

import static jakarta.persistence.CascadeType.ALL;
import static jakarta.persistence.FetchType.LAZY;
import static jakarta.persistence.GenerationType.SEQUENCE;
import static lombok.AccessLevel.PROTECTED;

@Entity
@Table(name = "modules", indexes = @Index(name = "ix_modules_course", columnList = "course_id"))
@Getter
@NoArgsConstructor(access = PROTECTED)
public class ModuleEntity {
    @Id
    @GeneratedValue(strategy = SEQUENCE)
    private Long id;

    @ManyToOne(fetch = LAZY, optional = false)
    @JoinColumn(name = "course_id", nullable = false)
    private CourseEntity course;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "text")
    private String description;

    @Column(nullable = false)
    private int orderIndex;

    @OneToMany(mappedBy = "module", fetch = LAZY, cascade = ALL, orphanRemoval = true)
    @OrderBy("orderIndex ASC, id ASC")
    private List<LessonEntity> lessons = new ArrayList<>();

    ModuleEntity(CourseEntity course, ModuleDetails details) {
        this.course = course;
        update(details);
    }

    public void update(ModuleDetails details) {
        title = details.title();
        description = details.description();
        orderIndex = details.orderIndex();
    }

    public LessonEntity addLesson(LessonDetails details) {
        final var lesson = new LessonEntity(this, details);
        lessons.add(lesson);
        return lesson;
    }

    public void removeLesson(LessonEntity lesson) {
        lessons.remove(lesson);
    }
}
