package com.facochico.edu.repository.quiz;

import com.facochico.edu.domain.quiz.dto.QuizDefinitionView;
import com.facochico.edu.domain.quiz.dto.QuizDetails;
import com.facochico.edu.domain.quiz.dto.QuizSubmissionView;
import com.facochico.edu.domain.quiz.dto.QuizView;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public interface QuizRepository {

    QuizDefinitionView insertQuiz(long moduleId, long teacherId, QuizDetails details);

    QuizDefinitionView findDefinitionByModuleId(long moduleId, long teacherId);

    QuizDefinitionView updateQuiz(long quizId, long teacherId, QuizDetails details);

    void deleteQuiz(long quizId, long teacherId);

    QuizView findQuizByModuleId(long moduleId, long studentId);

    QuizSubmissionView takeQuiz(long quizId, long studentId, Map<Long, Long> answers, Instant takenAt);

    QuizSubmissionView findResultById(long id, long actorId);

    List<QuizSubmissionView> listResultsByQuiz(long quizId, long teacherId, int page, int size);

    List<QuizSubmissionView> listResultsByStudent(long studentId, int page, int size);
}
