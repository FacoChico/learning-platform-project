package com.facochico.edu.domain.course;

import com.facochico.edu.domain.course.dto.CourseDetails;
import com.facochico.edu.domain.course.dto.ModuleDetails;
import com.facochico.edu.domain.user.UserEntity;
import com.facochico.edu.util.course.CurriculumValidationUtil;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
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
@Table(name = "courses", indexes = {
    @Index(name = "ix_courses_category", columnList = "category_id"),
    @Index(name = "ix_courses_teacher", columnList = "teacher_id")
})
@Getter
@NoArgsConstructor(access = PROTECTED)
public class CourseEntity {
    public static final String CATEGORY_FOREIGN_KEY = "fk_courses_category";
    public static final String TAG_FOREIGN_KEY = "fk_course_tags_tag";

    @Id
    @GeneratedValue(strategy = SEQUENCE)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "text")
    private String description;

    @ManyToOne(fetch = LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false, foreignKey = @ForeignKey(name = CATEGORY_FOREIGN_KEY))
    private CategoryEntity category;

    @ManyToOne(fetch = LAZY, optional = false)
    @JoinColumn(name = "teacher_id", nullable = false)
    private UserEntity teacher;

    @OneToMany(mappedBy = "course", fetch = LAZY, cascade = ALL, orphanRemoval = true)
    @OrderBy("orderIndex ASC, id ASC")
    private List<ModuleEntity> modules = new ArrayList<>();

    @ManyToMany(fetch = LAZY)
    @JoinTable(
        name = "course_tags",
        joinColumns = @JoinColumn(name = "course_id"),
        inverseJoinColumns = @JoinColumn(name = "tag_id"),
        inverseForeignKey = @ForeignKey(name = TAG_FOREIGN_KEY),
        indexes = @Index(name = "ix_course_tags_tag", columnList = "tag_id")
    )
    private Set<TagEntity> tags = new HashSet<>();

    public CourseEntity(CourseDetails details, CategoryEntity category, UserEntity teacher, Collection<TagEntity> tags) {
        update(details, category, teacher, tags);
    }

    public void update(CourseDetails details, CategoryEntity category, UserEntity teacher, Collection<TagEntity> tags) {
        CurriculumValidationUtil.requireTeacher(teacher.getRole());

        title = details.title();
        description = details.description();
        this.category = category;
        this.teacher = teacher;
        this.tags.retainAll(tags);
        this.tags.addAll(tags);
    }

    public ModuleEntity addModule(ModuleDetails details) {
        final var module = new ModuleEntity(this, details);
        modules.add(module);
        return module;
    }

    public void removeModule(ModuleEntity module) {
        modules.remove(module);
    }
}
