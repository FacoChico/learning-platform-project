package com.facochico.edu.domain.quiz;

import com.facochico.edu.domain.course.ModuleEntity;
import com.facochico.edu.domain.quiz.dto.QuizDetails;

import org.hibernate.annotations.OnDelete;

import java.util.LinkedHashSet;
import java.util.Set;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

import static jakarta.persistence.CascadeType.ALL;
import static jakarta.persistence.FetchType.LAZY;
import static jakarta.persistence.GenerationType.SEQUENCE;
import static lombok.AccessLevel.PROTECTED;
import static org.hibernate.annotations.OnDeleteAction.CASCADE;

@Entity
@Table(name = "quizzes")
@Getter
@NoArgsConstructor(access = PROTECTED)
public class QuizEntity {
    @Id
    @GeneratedValue(strategy = SEQUENCE)
    private Long id;

    @OneToOne(fetch = LAZY, optional = false)
    @JoinColumn(name = "module_id", nullable = false)
    @OnDelete(action = CASCADE)
    private ModuleEntity module;

    @Column(nullable = false)
    private String title;

    @OneToMany(mappedBy = "quiz", fetch = LAZY, cascade = ALL, orphanRemoval = true)
    @OrderBy("orderIndex ASC")
    private Set<QuestionEntity> questions = new LinkedHashSet<>();

    public QuizEntity(ModuleEntity module, QuizDetails details) {
        this.module = module;
        update(details);
    }

    public void update(QuizDetails details) {
        title = details.title();
        questions.clear();
        for (var index = 0; index < details.questions().size(); index++) {
            questions.add(new QuestionEntity(this, index, details.questions().get(index)));
        }
    }
}
