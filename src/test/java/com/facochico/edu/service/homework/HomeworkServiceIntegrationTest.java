package com.facochico.edu.service.homework;

import com.facochico.edu.domain.course.dto.CourseDetails;
import com.facochico.edu.domain.course.dto.LessonDetails;
import com.facochico.edu.domain.course.dto.ModuleDetails;
import com.facochico.edu.domain.homework.dto.AssignmentDetails;
import com.facochico.edu.domain.homework.exception.DuplicateSubmissionException;
import com.facochico.edu.domain.homework.exception.HomeworkAccessDeniedException;
import com.facochico.edu.domain.homework.exception.HomeworkInUseException;
import com.facochico.edu.domain.homework.exception.HomeworkNotFoundException;
import com.facochico.edu.domain.homework.exception.InvalidScoreException;
import com.facochico.edu.domain.homework.exception.SubmissionDeadlinePassedException;
import com.facochico.edu.domain.user.UserDetails;
import com.facochico.edu.service.course.CourseService;
import com.facochico.edu.service.enrollment.EnrollmentService;
import com.facochico.edu.service.user.UserService;
import com.facochico.edu.testutil.WithPostgres;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Executors;
import java.util.stream.Stream;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;

import static com.facochico.edu.domain.homework.SubmissionStatus.GRADED;
import static com.facochico.edu.domain.homework.SubmissionStatus.SUBMITTED;
import static com.facochico.edu.domain.user.UserRole.STUDENT;
import static com.facochico.edu.domain.user.UserRole.TEACHER;
import static java.time.ZoneOffset.UTC;
import static java.time.temporal.ChronoUnit.DAYS;
import static java.time.temporal.ChronoUnit.MICROS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static lombok.AccessLevel.PACKAGE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.Assertions.tuple;

@SpringBootTest
@WithPostgres
@Import(HomeworkServiceIntegrationTest.FixedClockConfig.class)
@RequiredArgsConstructor(access = PACKAGE, onConstructor_ = @Autowired)
class HomeworkServiceIntegrationTest {
    private static final Instant NOW = Instant.parse("2026-11-27T17:00:00Z");
    private static final Instant TOMORROW = NOW.plus(1, DAYS);

    private final HomeworkService homework;
    private final CourseService courses;
    private final EnrollmentService enrollments;
    private final UserService users;

    @Test
    void shouldReadUpdatedAssignmentWhenCourseTeacherEditsIt() {
        final var fixture = createLesson("edit");
        enrollments.enroll(fixture.courseId(), fixture.studentId());
        final var assignment = homework.createAssignment(fixture.lessonId(), fixture.teacherId(), assignmentAt(TOMORROW));
        final var updatedDetails = new AssignmentDetails("Задача про списки", "Обработать пустой список", TOMORROW, 15);

        final var updated = homework.updateAssignment(assignment.id(), fixture.teacherId(), updatedDetails);

        assertThat(updated.details()).isEqualTo(updatedDetails);
        assertThat(homework.findAssignment(assignment.id(), fixture.studentId())).isEqualTo(updated);
        assertThat(homework.listAssignments(fixture.lessonId(), fixture.studentId(), 0, 10)).containsExactly(updated);
    }

    @Test
    void shouldRemoveAssignmentWhenNoStudentHasSubmittedIt() {
        final var fixture = createLesson("delete");
        final var assignment = homework.createAssignment(fixture.lessonId(), fixture.teacherId(), assignmentAt(TOMORROW));

        homework.deleteAssignment(assignment.id(), fixture.teacherId());

        assertThat(homework.listAssignments(fixture.lessonId(), fixture.teacherId(), 0, 10)).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 10})
    void shouldShowGradeToStudentWhenTeacherGradesSubmission(int score) {
        final var fixture = createLesson("grade-" + score);
        final var other = users.create(new UserDetails("Мишаня", "misha.grade-" + score + "@example.org", STUDENT, null));
        enrollments.enroll(fixture.courseId(), fixture.studentId());
        enrollments.enroll(fixture.courseId(), other.getId());
        final var assignment = homework.createAssignment(fixture.lessonId(), fixture.teacherId(), assignmentAt(TOMORROW));
        final var submitted = homework.submit(assignment.id(), fixture.studentId(), "Сначала проверяю пустой список");
        final var otherSubmission = homework.submit(assignment.id(), other.getId(), "Решение через цикл");

        final var graded = homework.gradeSubmission(submitted.id(), fixture.teacherId(), score, "Добавить тест на пустой список");

        assertThat(submitted.status()).isEqualTo(SUBMITTED);
        assertThat(submitted.score()).isNull();
        assertThat(graded.status()).isEqualTo(GRADED);
        assertThat(graded.score()).isEqualTo(score);
        assertThat(graded.feedback()).isEqualTo("Добавить тест на пустой список");
        assertThat(homework.findSubmission(graded.id(), fixture.studentId())).isEqualTo(graded);
        assertThat(homework.listSubmissionsByStudent(fixture.studentId(), 0, 10)).containsExactly(graded);
        assertThat(homework.listSubmissionsByAssignment(assignment.id(), fixture.teacherId(), 0, 10))
            .containsExactly(graded, otherSubmission);
    }

    @Test
    void shouldAcceptSubmissionWhenItArrivesAtDeadline() {
        final var fixture = createLesson("deadline");
        enrollments.enroll(fixture.courseId(), fixture.studentId());
        final var assignment = homework.createAssignment(fixture.lessonId(), fixture.teacherId(), assignmentAt(NOW));

        final var submitted = homework.submit(assignment.id(), fixture.studentId(), "Решение из двух частей");

        assertThat(submitted.submittedAt()).isEqualTo(NOW);
        assertThat(homework.findSubmission(submitted.id(), fixture.studentId())).isEqualTo(submitted);
    }

    @Test
    void shouldRejectSubmissionWhenDeadlineHasPassed() {
        final var fixture = createLesson("late");
        enrollments.enroll(fixture.courseId(), fixture.studentId());
        final var details = assignmentAt(NOW.minus(1, MICROS));
        final var assignment = homework.createAssignment(fixture.lessonId(), fixture.teacherId(), details);

        final var failure = catchThrowable(() -> homework.submit(assignment.id(), fixture.studentId(), "Готовое решение"));

        assertThat(failure).isInstanceOf(SubmissionDeadlinePassedException.class);
        assertThat(homework.listSubmissionsByStudent(fixture.studentId(), 0, 10)).isEmpty();
    }

    @Test
    void shouldRejectSubmissionWhenTextIsBlank() {
        final var fixture = createLesson("blank");
        enrollments.enroll(fixture.courseId(), fixture.studentId());
        final var assignment = homework.createAssignment(fixture.lessonId(), fixture.teacherId(), assignmentAt(TOMORROW));

        final var failure = catchThrowable(() -> homework.submit(assignment.id(), fixture.studentId(), "  "));

        assertThat(failure).isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Submission content is required");
        assertThat(homework.listSubmissionsByStudent(fixture.studentId(), 0, 10)).isEmpty();
    }

    @Test
    void shouldRejectSecondSubmissionWhenStudentAlreadySubmittedWork() {
        final var fixture = createLesson("duplicate");
        enrollments.enroll(fixture.courseId(), fixture.studentId());
        final var assignment = homework.createAssignment(fixture.lessonId(), fixture.teacherId(), assignmentAt(TOMORROW));
        final var original = homework.submit(assignment.id(), fixture.studentId(), "Первый вариант решения");

        final var failure = catchThrowable(() -> homework.submit(assignment.id(), fixture.studentId(), "Второй вариант"));

        assertThat(failure).isInstanceOf(DuplicateSubmissionException.class);
        assertThat(homework.listSubmissionsByStudent(fixture.studentId(), 0, 10)).containsExactly(original);
    }

    @Test
    void shouldDenyAssignmentAccessWhenStudentIsNotEnrolled() {
        final var fixture = createLesson("not-enrolled");
        final var assignment = homework.createAssignment(fixture.lessonId(), fixture.teacherId(), assignmentAt(TOMORROW));

        final var readFailure = catchThrowable(() -> homework.findAssignment(assignment.id(), fixture.studentId()));
        final var listFailure = catchThrowable(() -> homework.listAssignments(fixture.lessonId(), fixture.studentId(), 0, 1));
        final var submitFailure = catchThrowable(() -> homework.submit(assignment.id(), fixture.studentId(), "Моё решение"));

        assertThat(readFailure).isInstanceOf(HomeworkAccessDeniedException.class);
        assertThat(listFailure).isInstanceOf(HomeworkAccessDeniedException.class);
        assertThat(submitFailure).isInstanceOf(HomeworkAccessDeniedException.class);
    }

    @Test
    void shouldRejectAssignmentCreationWhenTeacherDoesNotTeachCourse() {
        final var fixture = createLesson("create-access");
        final var other = users.create(new UserDetails("Илья", "ilya.other-homework@example.org", TEACHER, null));
        final var details = assignmentAt(TOMORROW);

        final var failure = catchThrowable(() -> homework.createAssignment(fixture.lessonId(), other.getId(), details));

        assertThat(failure).isInstanceOf(HomeworkAccessDeniedException.class);
        assertThat(homework.listAssignments(fixture.lessonId(), fixture.teacherId(), 0, 10)).isEmpty();
    }

    @Test
    void shouldDenyGradingAndListingWhenTeacherDoesNotTeachCourse() {
        final var fixture = createLesson("grade-access");
        final var other = users.create(new UserDetails("Анна", "anna.other-homework@example.org", TEACHER, null));
        enrollments.enroll(fixture.courseId(), fixture.studentId());
        final var assignment = homework.createAssignment(fixture.lessonId(), fixture.teacherId(), assignmentAt(TOMORROW));
        final var submission = homework.submit(assignment.id(), fixture.studentId(), "Решение с проверкой входных данных");

        final var gradeFailure = catchThrowable(() -> homework.gradeSubmission(submission.id(), other.getId(), 9, "Зачтено"));
        final var listFailure = catchThrowable(() -> homework.listSubmissionsByAssignment(assignment.id(), other.getId(), 0, 1));

        assertThat(gradeFailure).isInstanceOf(HomeworkAccessDeniedException.class);
        assertThat(listFailure).isInstanceOf(HomeworkAccessDeniedException.class);
        assertThat(homework.findSubmission(submission.id(), fixture.studentId())).isEqualTo(submission);
    }

    @Test
    void shouldDenyReadingSubmissionWhenItBelongsToAnotherStudent() {
        final var fixture = createLesson("read-access");
        final var other = users.create(new UserDetails("Валя", "valya.other-homework@example.org", STUDENT, null));
        enrollments.enroll(fixture.courseId(), fixture.studentId());
        enrollments.enroll(fixture.courseId(), other.getId());
        final var assignment = homework.createAssignment(fixture.lessonId(), fixture.teacherId(), assignmentAt(TOMORROW));
        final var submission = homework.submit(assignment.id(), fixture.studentId(), "Моё решение");

        final var failure = catchThrowable(() -> homework.findSubmission(submission.id(), other.getId()));

        assertThat(failure).isInstanceOf(HomeworkAccessDeniedException.class);
    }

    @ParameterizedTest
    @ValueSource(ints = {-1, 11})
    void shouldRejectScoreWhenItFallsOutsideAssignmentRange(int score) {
        final var fixture = createLesson("score-" + score);
        enrollments.enroll(fixture.courseId(), fixture.studentId());
        final var assignment = homework.createAssignment(fixture.lessonId(), fixture.teacherId(), assignmentAt(TOMORROW));
        final var submission = homework.submit(assignment.id(), fixture.studentId(), "Функция возвращает длину списка");

        final var failure = catchThrowable(() -> homework.gradeSubmission(submission.id(), fixture.teacherId(), score, null));

        assertThat(failure).isInstanceOf(InvalidScoreException.class);
        assertThat(homework.findSubmission(submission.id(), fixture.studentId()).status()).isEqualTo(SUBMITTED);
    }

    @Test
    void shouldKeepGradeWithinMaximumWhenScoreAndLimitChangeConcurrently() {
        final var fixture = createLesson("score-race");
        enrollments.enroll(fixture.courseId(), fixture.studentId());
        final var details = assignmentAt(TOMORROW);
        final var assignment = homework.createAssignment(fixture.lessonId(), fixture.teacherId(), details);
        final var submission = homework.submit(assignment.id(), fixture.studentId(), "Решение работает на примерах");
        final var changed = new AssignmentDetails(details.title(), details.description(), details.dueAt(), 8);
        final var ready = new CyclicBarrier(2);
        final Callable<Throwable> grade = () -> {
            ready.await(5, SECONDS);
            return catchThrowable(() -> homework.gradeSubmission(submission.id(), fixture.teacherId(), 9, null));
        };
        final Callable<Throwable> changeMax = () -> {
            ready.await(5, SECONDS);
            return catchThrowable(() -> homework.updateAssignment(assignment.id(), fixture.teacherId(), changed));
        };

        final var failures = runConcurrently(grade, changeMax);

        assertThat(failures).singleElement().isInstanceOf(InvalidScoreException.class);
        final var stored = homework.findSubmission(submission.id(), fixture.studentId());
        final var maxScore = homework.findAssignment(assignment.id(), fixture.teacherId()).details().maxScore();
        assertThat(tuple(maxScore, stored.score())).isIn(tuple(8, null), tuple(10, 9));
    }

    @Test
    void shouldKeepHomeworkWhenDeletionConflictsWithSubmission() {
        final var fixture = createLesson("protected");
        enrollments.enroll(fixture.courseId(), fixture.studentId());
        final var assignment = homework.createAssignment(fixture.lessonId(), fixture.teacherId(), assignmentAt(TOMORROW));
        final var submission = homework.submit(assignment.id(), fixture.studentId(), "Решение с примерами");
        enrollments.withdraw(fixture.courseId(), fixture.studentId());

        final var assignmentFailure = catchThrowable(() -> homework.deleteAssignment(assignment.id(), fixture.teacherId()));
        final var lessonFailure = catchThrowable(() -> courses.deleteLesson(fixture.lessonId()));
        final var moduleFailure = catchThrowable(() -> courses.deleteModule(fixture.moduleId()));
        final var courseFailure = catchThrowable(() -> courses.deleteCourse(fixture.courseId()));

        assertThat(assignmentFailure).isInstanceOf(HomeworkInUseException.class);
        assertThat(lessonFailure).isInstanceOf(HomeworkInUseException.class);
        assertThat(moduleFailure).isInstanceOf(HomeworkInUseException.class);
        assertThat(courseFailure).isInstanceOf(HomeworkInUseException.class);
        assertThat(homework.findAssignment(assignment.id(), fixture.teacherId())).isEqualTo(assignment);
        assertThat(homework.findSubmission(submission.id(), fixture.studentId())).isEqualTo(submission);
    }

    @Test
    void shouldDeleteAssignmentsWhenCourseWithoutSubmissionsIsRemoved() {
        final var fixture = createLesson("cascade");
        final var assignment = homework.createAssignment(fixture.lessonId(), fixture.teacherId(), assignmentAt(TOMORROW));

        courses.deleteCourse(fixture.courseId());

        assertThatExceptionOfType(HomeworkNotFoundException.class)
            .isThrownBy(() -> homework.findAssignment(assignment.id(), fixture.teacherId()));
    }

    private HomeworkFixture createLesson(String suffix) {
        final var teacher = users.create(new UserDetails("Таня", "tanya." + suffix + ".hw@example.org", TEACHER, null));
        final var student = users.create(new UserDetails("Гришаня", "grisha." + suffix + ".hw@example.org", STUDENT, null));
        final var category = courses.createCategory("Программирование");
        final var course = courses.createCourse(new CourseDetails("Практика Java", null, category.id(), teacher.getId(), Set.of()));
        final var module = courses.addModule(course.id(), new ModuleDetails("Коллекции", null, 0));
        final var lesson = courses.addLesson(module.id(), new LessonDetails("Списки", null, null, 0));
        return new HomeworkFixture(course.id(), module.id(), lesson.id(), teacher.getId(), student.getId());
    }

    private static AssignmentDetails assignmentAt(Instant deadline) {
        return new AssignmentDetails("Работа со списками", "Решить задачу из конспекта", deadline, 10);
    }

    @SneakyThrows
    private static List<Throwable> runConcurrently(Callable<Throwable> first, Callable<Throwable> second) {
        try (final var workers = Executors.newFixedThreadPool(2)) {
            final var firstResult = workers.submit(first);
            final var secondResult = workers.submit(second);
            return Stream.of(firstResult.get(10, SECONDS), secondResult.get(10, SECONDS))
                .filter(Objects::nonNull)
                .toList();
        }
    }

    private record HomeworkFixture(long courseId, long moduleId, long lessonId, long teacherId, long studentId) {
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class FixedClockConfig {

        @Bean
        @Primary
        static Clock fixedClock() {
            return Clock.fixed(NOW, UTC);
        }
    }
}
