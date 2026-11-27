package com.facochico.edu.domain.course;

import com.facochico.edu.domain.course.dto.LessonDetails;

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

@Entity
@Table(name = "lessons", indexes = @Index(name = "ix_lessons_module", columnList = "module_id"))
@Getter
@NoArgsConstructor(access = PROTECTED)
public class LessonEntity {
    @Id
    @GeneratedValue(strategy = SEQUENCE)
    private Long id;

    @ManyToOne(fetch = LAZY, optional = false)
    @JoinColumn(name = "module_id", nullable = false)
    private ModuleEntity module;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "text")
    private String content;

    @Column(columnDefinition = "text")
    private String videoUrl;

    @Column(nullable = false)
    private int orderIndex;

    LessonEntity(ModuleEntity module, LessonDetails details) {
        this.module = module;
        update(details);
    }

    public void update(LessonDetails details) {
        title = details.title();
        content = details.content();
        videoUrl = details.videoUrl();
        orderIndex = details.orderIndex();
    }
}
