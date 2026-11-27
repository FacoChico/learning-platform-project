package com.facochico.edu.service.quiz;

import com.facochico.edu.domain.course.dto.CourseDetails;
import com.facochico.edu.domain.course.dto.ModuleDetails;
import com.facochico.edu.domain.quiz.dto.QuizDetails;
import com.facochico.edu.domain.quiz.dto.QuizDetails.OptionDetails;
import com.facochico.edu.domain.quiz.dto.QuizDetails.QuestionDetails;
import com.facochico.edu.domain.quiz.dto.QuizView;
import com.facochico.edu.domain.quiz.dto.QuizView.OptionView;
import com.facochico.edu.domain.quiz.dto.QuizView.QuestionView;
import com.facochico.edu.domain.quiz.exception.DuplicateQuizException;
import com.facochico.edu.domain.quiz.exception.InvalidQuizAnswersException;
import com.facochico.edu.domain.quiz.exception.QuizAccessDeniedException;
import com.facochico.edu.domain.quiz.exception.QuizInUseException;
import com.facochico.edu.domain.quiz.exception.QuizNotFoundException;
import com.facochico.edu.domain.user.UserDetails;
import com.facochico.edu.service.course.CourseService;
import com.facochico.edu.service.enrollment.EnrollmentService;
import com.facochico.edu.service.user.UserService;
import com.facochico.edu.testutil.WithPostgres;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Executors;
import java.util.stream.Stream;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;

import static com.facochico.edu.domain.user.UserRole.STUDENT;
import static com.facochico.edu.domain.user.UserRole.TEACHER;
import static java.util.concurrent.TimeUnit.SECONDS;
import static java.util.stream.Collectors.toUnmodifiableMap;
import static lombok.AccessLevel.PACKAGE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.catchThrowable;

@SpringBootTest
@WithPostgres
@RequiredArgsConstructor(access = PACKAGE, onConstructor_ = @Autowired)
class QuizServiceIntegrationTest {
    private final QuizService quizzes;
    private final CourseService courses;
    private final EnrollmentService enrollments;
    private final UserService users;

    @Test
    void shouldReadOrderedQuestionsWhenTeacherCreatesQuiz() {
        final var fixture = createModule("definition");
        final var details = quizDetails();
        enrollments.enroll(fixture.courseId(), fixture.studentId());

        final var created = quizzes.createQuiz(fixture.moduleId(), fixture.teacherId(), details);

        final var definition = quizzes.findDefinitionByModuleId(fixture.moduleId(), fixture.teacherId());
        final var view = quizzes.findQuizByModuleId(fixture.moduleId(), fixture.studentId());
        assertThat(definition).isEqualTo(created);
        assertThat(definition.details()).isEqualTo(details);
        assertThat(view.id()).isEqualTo(created.id());
        assertThat(view.questions()).extracting(QuestionView::text)
            .containsExactly("Какая команда читает строки таблицы?", "Как пропустить повторяющиеся строки результата?");
        assertThat(view.questions().getFirst().options()).extracting(OptionView::text)
            .containsExactly("SELECT", "UPDATE");
        assertThat(view.questions().getLast().options()).extracting(OptionView::text)
            .containsExactly("DISTINCT", "ORDER BY");
    }

    @Test
    void shouldKeepBothResultsWhenStudentRetakesQuiz() {
        final var fixture = createModule("retake");
        final var quiz = quizzes.createQuiz(fixture.moduleId(), fixture.teacherId(), quizDetails());
        final var other = users.create(new UserDetails("Мишаня", "misha.retake@example.org", STUDENT, null));
        enrollments.enroll(fixture.courseId(), fixture.studentId());
        enrollments.enroll(fixture.courseId(), other.getId());
        final var view = quizzes.findQuizByModuleId(fixture.moduleId(), fixture.studentId());

        final var first = quizzes.takeQuiz(quiz.id(), fixture.studentId(), chooseOptions(view, 1));
        final var otherResult = quizzes.takeQuiz(quiz.id(), other.getId(), chooseOptions(view, 0));
        final var second = quizzes.takeQuiz(quiz.id(), fixture.studentId(), chooseOptions(view, 0));

        assertThat(first.score()).isZero();
        assertThat(first.maxScore()).isEqualTo(2);
        assertThat(second.score()).isEqualTo(2);
        assertThat(second.maxScore()).isEqualTo(2);
        assertThat(second.takenAt()).isNotNull();
        assertThat(quizzes.listResultsByStudent(fixture.studentId(), 0, 10)).containsExactly(second, first);
        assertThat(quizzes.listResultsByQuiz(quiz.id(), fixture.teacherId(), 0, 2)).containsExactly(second, otherResult);
        assertThat(quizzes.listResultsByQuiz(quiz.id(), fixture.teacherId(), 1, 2)).containsExactly(first);
    }

    @Test
    void shouldKeepStoredScoreWhenTeacherReplacesQuestions() {
        final var fixture = createModule("edit");
        enrollments.enroll(fixture.courseId(), fixture.studentId());
        final var quiz = quizzes.createQuiz(fixture.moduleId(), fixture.teacherId(), quizDetails());
        final var original = quizzes.findQuizByModuleId(fixture.moduleId(), fixture.studentId());
        final var first = quizzes.takeQuiz(quiz.id(), fixture.studentId(), chooseOptions(original, 0));
        final var changed = new QuizDetails("Агрегатные функции", List.of(new QuestionDetails(
            "Какая функция считает строки?", List.of(new OptionDetails("COUNT", true), new OptionDetails("MAX", false))
        )));

        final var updated = quizzes.updateQuiz(quiz.id(), fixture.teacherId(), changed);
        final var newView = quizzes.findQuizByModuleId(fixture.moduleId(), fixture.studentId());
        final var second = quizzes.takeQuiz(quiz.id(), fixture.studentId(), chooseOptions(newView, 0));

        assertThat(updated.details()).isEqualTo(changed);
        assertThat(newView.questions()).extracting(QuestionView::text).containsExactly("Какая функция считает строки?");
        assertThat(second.maxScore()).isEqualTo(1);
        assertThat(second.score()).isEqualTo(1);
        assertThat(first.score()).isEqualTo(2);
        assertThat(first.maxScore()).isEqualTo(2);
        assertThat(quizzes.listResultsByStudent(fixture.studentId(), 0, 10)).containsExactly(second, first);
    }

    @Test
    void shouldRejectSecondQuizWhenModuleAlreadyHasOne() {
        final var fixture = createModule("duplicate");
        final var original = quizzes.createQuiz(fixture.moduleId(), fixture.teacherId(), quizDetails());

        final var failure = catchThrowable(() -> quizzes.createQuiz(fixture.moduleId(), fixture.teacherId(), quizDetails()));

        assertThat(failure).isInstanceOf(DuplicateQuizException.class);
        assertThat(quizzes.findDefinitionByModuleId(fixture.moduleId(), fixture.teacherId())).isEqualTo(original);
    }

    @Test
    @SneakyThrows
    void shouldCreateOneQuizWhenTwoRequestsTargetSameModule() {
        final var fixture = createModule("concurrent");
        final var details = quizDetails();
        final var ready = new CyclicBarrier(2);
        final Callable<Throwable> request = () -> {
            ready.await(5, SECONDS);
            return catchThrowable(() -> quizzes.createQuiz(fixture.moduleId(), fixture.teacherId(), details));
        };

        final List<Throwable> failures;
        try (final var workers = Executors.newFixedThreadPool(2)) {
            final var first = workers.submit(request);
            final var second = workers.submit(request);
            failures = Stream.of(first.get(10, SECONDS), second.get(10, SECONDS))
                .filter(Objects::nonNull)
                .toList();
        }

        assertThat(failures).singleElement().isInstanceOf(DuplicateQuizException.class);
        assertThat(quizzes.findDefinitionByModuleId(fixture.moduleId(), fixture.teacherId()).details()).isEqualTo(details);
    }

    @Test
    void shouldDenyQuizAccessWhenStudentWithdraws() {
        final var fixture = createModule("withdraw");
        final var quiz = quizzes.createQuiz(fixture.moduleId(), fixture.teacherId(), quizDetails());
        enrollments.enroll(fixture.courseId(), fixture.studentId());
        final var view = quizzes.findQuizByModuleId(fixture.moduleId(), fixture.studentId());
        enrollments.withdraw(fixture.courseId(), fixture.studentId());

        final var viewFailure = catchThrowable(() -> quizzes.findQuizByModuleId(fixture.moduleId(), fixture.studentId()));
        final var attemptFailure = catchThrowable(() -> quizzes.takeQuiz(quiz.id(), fixture.studentId(), chooseOptions(view, 0)));

        assertThat(viewFailure).isInstanceOf(QuizAccessDeniedException.class);
        assertThat(attemptFailure).isInstanceOf(QuizAccessDeniedException.class);
        assertThat(quizzes.listResultsByStudent(fixture.studentId(), 0, 10)).isEmpty();
    }

    @Test
    void shouldDenyQuizDefinitionWhenActorIsEnrolledStudent() {
        final var fixture = createModule("answer-key");
        quizzes.createQuiz(fixture.moduleId(), fixture.teacherId(), quizDetails());
        enrollments.enroll(fixture.courseId(), fixture.studentId());

        final var failure = catchThrowable(() -> quizzes.findDefinitionByModuleId(fixture.moduleId(), fixture.studentId()));

        assertThat(failure).isInstanceOf(QuizAccessDeniedException.class);
    }

    @Test
    void shouldRejectQuizManagementWhenTeacherDoesNotTeachCourse() {
        final var fixture = createModule("teacher");
        final var other = users.create(new UserDetails("Анна", "anna.other-quiz@example.org", TEACHER, null));
        final var original = quizzes.createQuiz(fixture.moduleId(), fixture.teacherId(), quizDetails());

        final var editFailure = catchThrowable(() -> quizzes.updateQuiz(original.id(), other.getId(), quizDetails()));
        final var resultsFailure = catchThrowable(() -> quizzes.listResultsByQuiz(original.id(), other.getId(), 0, 10));

        assertThat(editFailure).isInstanceOf(QuizAccessDeniedException.class);
        assertThat(resultsFailure).isInstanceOf(QuizAccessDeniedException.class);
        assertThat(quizzes.findDefinitionByModuleId(fixture.moduleId(), fixture.teacherId())).isEqualTo(original);
    }

    @Test
    void shouldLeaveResultsEmptyWhenSelectedOptionBelongsToAnotherQuestion() {
        final var fixture = createModule("wrong-option");
        final var quiz = quizzes.createQuiz(fixture.moduleId(), fixture.teacherId(), quizDetails());
        enrollments.enroll(fixture.courseId(), fixture.studentId());
        final var view = quizzes.findQuizByModuleId(fixture.moduleId(), fixture.studentId());
        final var first = view.questions().getFirst();
        final var second = view.questions().getLast();
        final var foreignOption = second.options().getFirst().id();
        final var answers = Map.of(first.id(), foreignOption, second.id(), foreignOption);

        final var failure = catchThrowable(() -> quizzes.takeQuiz(quiz.id(), fixture.studentId(), answers));

        assertThat(failure).isInstanceOf(InvalidQuizAnswersException.class);
        assertThat(quizzes.listResultsByQuiz(quiz.id(), fixture.teacherId(), 0, 10)).isEmpty();
    }

    @Test
    void shouldRemoveQuizWhenItHasNoResults() {
        final var fixture = createModule("delete");
        final var quiz = quizzes.createQuiz(fixture.moduleId(), fixture.teacherId(), quizDetails());

        quizzes.deleteQuiz(quiz.id(), fixture.teacherId());

        assertThatExceptionOfType(QuizNotFoundException.class)
            .isThrownBy(() -> quizzes.findDefinitionByModuleId(fixture.moduleId(), fixture.teacherId()));
    }

    @Test
    void shouldRemoveQuizWhenCourseWithoutResultsIsDeleted() {
        final var fixture = createModule("cascade");
        quizzes.createQuiz(fixture.moduleId(), fixture.teacherId(), quizDetails());

        courses.deleteCourse(fixture.courseId());

        assertThatExceptionOfType(QuizNotFoundException.class)
            .isThrownBy(() -> quizzes.findDefinitionByModuleId(fixture.moduleId(), fixture.teacherId()));
    }

    @Test
    void shouldKeepQuizAndResultsWhenDeletionConflictsWithAttempt() {
        final var fixture = createModule("protected");
        final var quiz = quizzes.createQuiz(fixture.moduleId(), fixture.teacherId(), quizDetails());
        enrollments.enroll(fixture.courseId(), fixture.studentId());
        final var view = quizzes.findQuizByModuleId(fixture.moduleId(), fixture.studentId());
        final var result = quizzes.takeQuiz(quiz.id(), fixture.studentId(), chooseOptions(view, 0));
        enrollments.withdraw(fixture.courseId(), fixture.studentId());

        final var quizFailure = catchThrowable(() -> quizzes.deleteQuiz(quiz.id(), fixture.teacherId()));
        final var moduleFailure = catchThrowable(() -> courses.deleteModule(fixture.moduleId()));
        final var courseFailure = catchThrowable(() -> courses.deleteCourse(fixture.courseId()));

        assertThat(quizFailure).isInstanceOf(QuizInUseException.class);
        assertThat(moduleFailure).isInstanceOf(QuizInUseException.class);
        assertThat(courseFailure).isInstanceOf(QuizInUseException.class);
        assertThat(quizzes.findDefinitionByModuleId(fixture.moduleId(), fixture.teacherId())).isEqualTo(quiz);
        assertThat(quizzes.listResultsByStudent(fixture.studentId(), 0, 10)).containsExactly(result);
    }

    private QuizFixture createModule(String suffix) {
        final var teacher = users.create(new UserDetails("Таня", "tanya." + suffix + ".quiz@example.org", TEACHER, null));
        final var student = users.create(new UserDetails("Гришаня", "grisha." + suffix + ".quiz@example.org", STUDENT, null));
        final var category = courses.createCategory("Базы данных");
        final var course = courses.createCourse(new CourseDetails("Практика SQL", null, category.id(), teacher.getId(), Set.of()));
        final var module = courses.addModule(course.id(), new ModuleDetails("Запросы", null, 0));
        return new QuizFixture(course.id(), module.id(), teacher.getId(), student.getId());
    }

    private static QuizDetails quizDetails() {
        return new QuizDetails("Проверка знаний по SQL", List.of(
            new QuestionDetails(
                "Какая команда читает строки таблицы?",
                List.of(new OptionDetails("SELECT", true), new OptionDetails("UPDATE", false))
            ),
            new QuestionDetails(
                "Как пропустить повторяющиеся строки результата?",
                List.of(new OptionDetails("DISTINCT", true), new OptionDetails("ORDER BY", false))
            )
        ));
    }

    private static Map<Long, Long> chooseOptions(QuizView quiz, int optionIndex) {
        return quiz.questions().stream()
            .collect(toUnmodifiableMap(QuestionView::id, question -> question.options().get(optionIndex).id()));
    }

    private record QuizFixture(long courseId, long moduleId, long teacherId, long studentId) {
    }
}
