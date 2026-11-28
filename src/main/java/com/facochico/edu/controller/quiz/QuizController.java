package com.facochico.edu.controller.quiz;

import com.facochico.edu.controller.dto.PageQuery;
import com.facochico.edu.controller.quiz.dto.QuizAttemptRequest;
import com.facochico.edu.domain.quiz.dto.QuizDefinitionView;
import com.facochico.edu.domain.quiz.dto.QuizDetails;
import com.facochico.edu.domain.quiz.dto.QuizSubmissionView;
import com.facochico.edu.domain.quiz.dto.QuizView;
import com.facochico.edu.service.quiz.QuizService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;

import lombok.RequiredArgsConstructor;

import static org.springframework.http.HttpStatus.NO_CONTENT;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
class QuizController {
    private final QuizService quizzes;

    @PostMapping("/modules/{moduleId}/quizzes")
    ResponseEntity<QuizDefinitionView> create(
        @PathVariable @Positive long moduleId,
        @RequestHeader("X-User-Id") @Positive long teacherId,
        @RequestBody QuizDetails details
    ) {
        final var quiz = quizzes.createQuiz(moduleId, teacherId, details);
        final var location = URI.create("/api/v1/modules/" + moduleId + "/quiz/definition");
        return ResponseEntity.created(location).body(quiz);
    }

    @GetMapping("/modules/{moduleId}/quiz/definition")
    QuizDefinitionView findDefinition(
        @PathVariable @Positive long moduleId,
        @RequestHeader("X-User-Id") @Positive long teacherId
    ) {
        return quizzes.findDefinitionByModuleId(moduleId, teacherId);
    }

    @GetMapping("/modules/{moduleId}/quiz")
    QuizView findForStudent(
        @PathVariable @Positive long moduleId,
        @RequestHeader("X-User-Id") @Positive long studentId
    ) {
        return quizzes.findQuizByModuleId(moduleId, studentId);
    }

    @PutMapping("/quizzes/{id}")
    QuizDefinitionView update(
        @PathVariable @Positive long id,
        @RequestHeader("X-User-Id") @Positive long teacherId,
        @RequestBody QuizDetails details
    ) {
        return quizzes.updateQuiz(id, teacherId, details);
    }

    @DeleteMapping("/quizzes/{id}")
    @ResponseStatus(NO_CONTENT)
    void delete(@PathVariable @Positive long id, @RequestHeader("X-User-Id") @Positive long teacherId) {
        quizzes.deleteQuiz(id, teacherId);
    }

    @PostMapping("/quizzes/{id}/results")
    ResponseEntity<QuizSubmissionView> takeQuiz(
        @PathVariable @Positive long id,
        @RequestHeader("X-User-Id") @Positive long studentId,
        @RequestBody @Valid QuizAttemptRequest request
    ) {
        final var result = quizzes.takeQuiz(id, studentId, request.answers());
        return ResponseEntity.created(URI.create("/api/v1/quiz-results/" + result.id())).body(result);
    }

    @GetMapping("/quizzes/{id}/results")
    List<QuizSubmissionView> listByQuiz(
        @PathVariable @Positive long id,
        @RequestHeader("X-User-Id") @Positive long teacherId,
        @ModelAttribute @Valid PageQuery page
    ) {
        return quizzes.listResultsByQuiz(id, teacherId, page.page(), page.size());
    }

    @GetMapping("/quiz-results")
    List<QuizSubmissionView> listByStudent(
        @RequestHeader("X-User-Id") @Positive long studentId,
        @ModelAttribute @Valid PageQuery page
    ) {
        return quizzes.listResultsByStudent(studentId, page.page(), page.size());
    }

    @GetMapping("/quiz-results/{id}")
    QuizSubmissionView findResult(@PathVariable @Positive long id, @RequestHeader("X-User-Id") @Positive long actorId) {
        return quizzes.findResultById(id, actorId);
    }
}
