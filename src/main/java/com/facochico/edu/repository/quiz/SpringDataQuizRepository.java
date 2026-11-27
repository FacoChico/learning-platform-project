package com.facochico.edu.repository.quiz;

import com.facochico.edu.domain.quiz.QuizEntity;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

import static jakarta.persistence.LockModeType.PESSIMISTIC_WRITE;

interface SpringDataQuizRepository extends JpaRepository<QuizEntity, Long> {

    boolean existsByModuleId(long moduleId);

    @EntityGraph(attributePaths = {"questions", "questions.options"})
    Optional<QuizEntity> findWithQuestionsByModuleId(long moduleId);

    @EntityGraph(attributePaths = {"questions", "questions.options"})
    Optional<QuizEntity> findWithQuestionsById(long id);

    // Scoring and editing use the same lock so an attempt sees one definition.
    @Lock(PESSIMISTIC_WRITE)
    @Query("select quiz from QuizEntity quiz where quiz.id = :id")
    Optional<QuizEntity> findForUpdate(@Param("id") long id);
}
