package com.facochico.edu.repository.quiz;

import com.facochico.edu.domain.quiz.QuizSubmissionEntity;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

interface SpringDataQuizSubmissionRepository extends JpaRepository<QuizSubmissionEntity, Long> {

    List<QuizSubmissionEntity> findByQuizIdOrderByIdDesc(long quizId, Pageable pageable);

    List<QuizSubmissionEntity> findByStudentIdOrderByIdDesc(long studentId, Pageable pageable);
}
