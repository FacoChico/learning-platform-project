package com.facochico.edu.domain.quiz;

import com.facochico.edu.domain.quiz.dto.QuizDetails.OptionDetails;

import org.hibernate.annotations.OnDelete;

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
@Table(name = "answer_options", indexes = @Index(name = "ix_answer_options_question", columnList = "question_id"))
@NoArgsConstructor(access = PROTECTED)
public class AnswerOptionEntity {
    @Getter
    @Id
    @GeneratedValue(strategy = SEQUENCE)
    private Long id;

    @ManyToOne(fetch = LAZY, optional = false)
    @JoinColumn(name = "question_id", nullable = false)
    @OnDelete(action = CASCADE)
    private QuestionEntity question;

    @Column(nullable = false)
    private int orderIndex;

    @Getter
    @Column(nullable = false, columnDefinition = "text")
    private String text;

    @Getter
    @Column(nullable = false)
    private boolean correct;

    AnswerOptionEntity(QuestionEntity question, int orderIndex, OptionDetails details) {
        this.question = question;
        this.orderIndex = orderIndex;
        text = details.text();
        correct = details.correct();
    }
}
