package com.facochico.edu.repository.quiz;

import com.facochico.edu.domain.course.ModuleEntity;
import com.facochico.edu.domain.course.exception.CurriculumItemNotFoundException;
import com.facochico.edu.domain.quiz.AnswerOptionEntity;
import com.facochico.edu.domain.quiz.QuestionEntity;
import com.facochico.edu.domain.quiz.QuizEntity;
import com.facochico.edu.domain.quiz.QuizSubmissionEntity;
import com.facochico.edu.domain.quiz.dto.QuizDefinitionView;
import com.facochico.edu.domain.quiz.dto.QuizDetails;
import com.facochico.edu.domain.quiz.dto.QuizDetails.OptionDetails;
import com.facochico.edu.domain.quiz.dto.QuizDetails.QuestionDetails;
import com.facochico.edu.domain.quiz.dto.QuizQuestionKey;
import com.facochico.edu.domain.quiz.dto.QuizSubmissionView;
import com.facochico.edu.domain.quiz.dto.QuizView;
import com.facochico.edu.domain.quiz.dto.QuizView.OptionView;
import com.facochico.edu.domain.quiz.dto.QuizView.QuestionView;
import com.facochico.edu.domain.quiz.exception.DuplicateQuizException;
import com.facochico.edu.domain.quiz.exception.QuizAccessDeniedException;
import com.facochico.edu.domain.quiz.exception.QuizInUseException;
import com.facochico.edu.domain.quiz.exception.QuizNotFoundException;
import com.facochico.edu.domain.user.UserEntity;
import com.facochico.edu.domain.user.exception.UserNotFoundException;
import com.facochico.edu.repository.course.EnrollmentRepository;
import com.facochico.edu.repository.course.SpringDataModuleRepository;
import com.facochico.edu.repository.user.SpringDataUserRepository;
import com.facochico.edu.util.persistence.ConstraintViolationUtil;
import com.facochico.edu.util.quiz.QuizScoringUtil;
import com.facochico.edu.util.user.UserValidationUtil;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import lombok.RequiredArgsConstructor;

import static com.facochico.edu.domain.quiz.QuizSubmissionEntity.QUIZ_FOREIGN_KEY;
import static com.facochico.edu.domain.user.UserRole.TEACHER;
import static java.util.stream.Collectors.toUnmodifiableSet;
import static lombok.AccessLevel.PACKAGE;

@Repository
@Transactional
@RequiredArgsConstructor(access = PACKAGE)
class JpaQuizRepository implements QuizRepository {
    private final SpringDataQuizRepository quizzes;
    private final SpringDataQuizSubmissionRepository submissions;
    private final SpringDataModuleRepository modules;
    private final SpringDataUserRepository users;
    private final EnrollmentRepository enrollments;

    @Override
    public QuizDefinitionView insertQuiz(long moduleId, long teacherId, QuizDetails details) {
        final var module = modules.findForUpdate(moduleId)
            .orElseThrow(() -> new CurriculumItemNotFoundException("Module", moduleId));
        requireCourseTeacher(module, teacherId);

        if (quizzes.existsByModuleId(moduleId)) {
            throw new DuplicateQuizException(moduleId);
        }

        final var quiz = quizzes.save(new QuizEntity(module, details));
        return toDefinitionView(quiz);
    }

    @Override
    @Transactional(readOnly = true)
    public QuizDefinitionView findDefinitionByModuleId(long moduleId, long teacherId) {
        final var quiz = requireQuizByModuleId(moduleId);
        requireCourseTeacher(quiz.getModule(), teacherId);

        return toDefinitionView(quiz);
    }

    @Override
    public QuizDefinitionView updateQuiz(long quizId, long teacherId, QuizDetails details) {
        final var quiz = lockQuiz(quizId);
        requireCourseTeacher(quiz.getModule(), teacherId);
        final var loaded = loadQuestions(quizId);

        loaded.update(details);
        return toDefinitionView(loaded);
    }

    @Override
    public void deleteQuiz(long quizId, long teacherId) {
        final var quiz = lockQuiz(quizId);
        requireCourseTeacher(quiz.getModule(), teacherId);

        try {
            quizzes.delete(quiz);
            quizzes.flush();
        } catch (DataIntegrityViolationException exception) {
            if (ConstraintViolationUtil.violatesConstraint(exception, QUIZ_FOREIGN_KEY)) {
                throw new QuizInUseException("Quiz", quizId, exception);
            }
            throw exception;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public QuizView findQuizByModuleId(long moduleId, long studentId) {
        final var quiz = requireQuizByModuleId(moduleId);
        requireEnrolledStudent(quiz.getModule(), studentId);

        return toQuizView(quiz);
    }

    @Override
    public QuizSubmissionView takeQuiz(long quizId, long studentId, Map<Long, Long> answers, Instant takenAt) {
        final var quiz = lockQuiz(quizId);
        final var student = requireEnrolledStudent(quiz.getModule(), studentId);
        final var loaded = loadQuestions(quizId);

        final var questions = loaded.getQuestions().stream()
            .map(JpaQuizRepository::toQuestionKey)
            .toList();
        final var result = QuizScoringUtil.score(questions, answers);
        final var submission = submissions.save(new QuizSubmissionEntity(quiz, student, result, takenAt));
        return toSubmissionView(submission);
    }

    @Override
    @Transactional(readOnly = true)
    public List<QuizSubmissionView> listResultsByQuiz(long quizId, long teacherId, int page, int size) {
        final var quiz = quizzes.findById(quizId)
            .orElseThrow(() -> quizNotFound(quizId));
        requireCourseTeacher(quiz.getModule(), teacherId);

        return submissions.findByQuizIdOrderByIdDesc(quizId, PageRequest.of(page, size)).stream()
            .map(JpaQuizRepository::toSubmissionView)
            .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<QuizSubmissionView> listResultsByStudent(long studentId, int page, int size) {
        final var student = requireUser(studentId);
        UserValidationUtil.requireStudent(student.getRole());

        return submissions.findByStudentIdOrderByIdDesc(studentId, PageRequest.of(page, size)).stream()
            .map(JpaQuizRepository::toSubmissionView)
            .toList();
    }

    private QuizEntity requireQuizByModuleId(long moduleId) {
        return quizzes.findWithQuestionsByModuleId(moduleId)
            .orElseThrow(() -> new QuizNotFoundException("Module %d has no quiz".formatted(moduleId)));
    }

    private QuizEntity lockQuiz(long quizId) {
        return quizzes.findForUpdate(quizId)
            .orElseThrow(() -> quizNotFound(quizId));
    }

    private QuizEntity loadQuestions(long quizId) {
        return quizzes.findWithQuestionsById(quizId)
            .orElseThrow(() -> quizNotFound(quizId));
    }

    private UserEntity requireUser(long id) {
        return users.findById(id)
            .orElseThrow(() -> new UserNotFoundException(id));
    }

    private void requireCourseTeacher(ModuleEntity module, long teacherId) {
        final var teacher = requireUser(teacherId);
        final var isCourseTeacher = teacher.getRole() == TEACHER
            && module.getCourse().getTeacher().getId().equals(teacher.getId());
        if (!isCourseTeacher) {
            throw new QuizAccessDeniedException("Only the course teacher may manage its quizzes and results");
        }
    }

    private UserEntity requireEnrolledStudent(ModuleEntity module, long studentId) {
        final var student = requireUser(studentId);
        UserValidationUtil.requireStudent(student.getRole());

        if (enrollments.findEnrollment(module.getCourse().getId(), studentId).isEmpty()) {
            throw new QuizAccessDeniedException("Only enrolled students may take or view the quiz");
        }
        return student;
    }

    private static QuizNotFoundException quizNotFound(long id) {
        return new QuizNotFoundException("Quiz %d was not found".formatted(id));
    }

    private static QuizDefinitionView toDefinitionView(QuizEntity quiz) {
        final var questions = quiz.getQuestions().stream()
            .map(JpaQuizRepository::toQuestionDetails)
            .toList();
        return new QuizDefinitionView(quiz.getId(), quiz.getModule().getId(), new QuizDetails(quiz.getTitle(), questions));
    }

    private static QuestionDetails toQuestionDetails(QuestionEntity question) {
        final var options = question.getOptions().stream()
            .map(option -> new OptionDetails(option.getText(), option.isCorrect()))
            .toList();
        return new QuestionDetails(question.getText(), options);
    }

    private static QuizView toQuizView(QuizEntity quiz) {
        final var questions = quiz.getQuestions().stream()
            .map(JpaQuizRepository::toQuestionView)
            .toList();
        return new QuizView(quiz.getId(), quiz.getModule().getId(), quiz.getTitle(), questions);
    }

    private static QuestionView toQuestionView(QuestionEntity question) {
        final var options = question.getOptions().stream()
            .map(option -> new OptionView(option.getId(), option.getText()))
            .toList();
        return new QuestionView(question.getId(), question.getText(), options);
    }

    private static QuizQuestionKey toQuestionKey(QuestionEntity question) {
        final var optionIds = question.getOptions().stream()
            .map(AnswerOptionEntity::getId)
            .collect(toUnmodifiableSet());
        final var correctOption = question.getOptions().stream()
            .filter(AnswerOptionEntity::isCorrect)
            .findFirst()
            .orElseThrow();
        return new QuizQuestionKey(question.getId(), optionIds, correctOption.getId());
    }

    private static QuizSubmissionView toSubmissionView(QuizSubmissionEntity submission) {
        return new QuizSubmissionView(
            submission.getId(), submission.getQuiz().getId(), submission.getStudent().getId(),
            submission.getScore(), submission.getMaxScore(), submission.getTakenAt()
        );
    }
}
