package com.facochico.edu.service.quiz;

import com.facochico.edu.domain.quiz.dto.QuizDefinitionView;
import com.facochico.edu.domain.quiz.dto.QuizDetails;
import com.facochico.edu.domain.quiz.dto.QuizSubmissionView;
import com.facochico.edu.domain.quiz.dto.QuizView;
import com.facochico.edu.repository.quiz.QuizRepository;
import com.facochico.edu.util.PaginationValidationUtil;
import com.facochico.edu.util.quiz.QuizValidationUtil;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import lombok.RequiredArgsConstructor;

import static java.time.temporal.ChronoUnit.MICROS;

@RequiredArgsConstructor
public class QuizService {
    private final QuizRepository quizzes;
    private final Clock clock;

    public QuizDefinitionView createQuiz(long moduleId, long teacherId, QuizDetails details) {
        return quizzes.insertQuiz(moduleId, teacherId, details);
    }

    public QuizDefinitionView findDefinitionByModuleId(long moduleId, long teacherId) {
        return quizzes.findDefinitionByModuleId(moduleId, teacherId);
    }

    public QuizDefinitionView updateQuiz(long quizId, long teacherId, QuizDetails details) {
        return quizzes.updateQuiz(quizId, teacherId, details);
    }

    public void deleteQuiz(long quizId, long teacherId) {
        quizzes.deleteQuiz(quizId, teacherId);
    }

    public QuizView findQuizByModuleId(long moduleId, long studentId) {
        return quizzes.findQuizByModuleId(moduleId, studentId);
    }

    public QuizSubmissionView takeQuiz(long quizId, long studentId, Map<Long, Long> answers) {
        final var selectedOptions = QuizValidationUtil.copyAnswers(answers);
        final var takenAt = Instant.now(clock).truncatedTo(MICROS);
        return quizzes.takeQuiz(quizId, studentId, selectedOptions, takenAt);
    }

    public List<QuizSubmissionView> listResultsByQuiz(long quizId, long teacherId, int page, int size) {
        PaginationValidationUtil.requirePage(page, size);
        return quizzes.listResultsByQuiz(quizId, teacherId, page, size);
    }

    public List<QuizSubmissionView> listResultsByStudent(long studentId, int page, int size) {
        PaginationValidationUtil.requirePage(page, size);
        return quizzes.listResultsByStudent(studentId, page, size);
    }
}
