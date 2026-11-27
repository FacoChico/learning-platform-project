package com.facochico.edu.domain.quiz;

import com.facochico.edu.domain.quiz.dto.QuizDetails.QuestionDetails;

import org.hibernate.annotations.OnDelete;

import java.util.LinkedHashSet;
import java.util.Set;

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
import static org.hibernate.annotations.OnDeleteAction.CASCADE;

@Entity
@Table(name = "questions", indexes = @Index(name = "ix_questions_quiz", columnList = "quiz_id"))
@NoArgsConstructor(access = PROTECTED)
public class QuestionEntity {
    @Getter
    @Id
    @GeneratedValue(strategy = SEQUENCE)
    private Long id;

    @ManyToOne(fetch = LAZY, optional = false)
    @JoinColumn(name = "quiz_id", nullable = false)
    @OnDelete(action = CASCADE)
    private QuizEntity quiz;

    @Column(nullable = false)
    private int orderIndex;

    @Getter
    @Column(nullable = false, columnDefinition = "text")
    private String text;

    @Getter
    @OneToMany(mappedBy = "question", fetch = LAZY, cascade = ALL, orphanRemoval = true)
    @OrderBy("orderIndex ASC")
    private Set<AnswerOptionEntity> options = new LinkedHashSet<>();

    QuestionEntity(QuizEntity quiz, int orderIndex, QuestionDetails details) {
        this.quiz = quiz;
        this.orderIndex = orderIndex;
        text = details.text();
        for (var index = 0; index < details.options().size(); index++) {
            options.add(new AnswerOptionEntity(this, index, details.options().get(index)));
        }
    }
}
