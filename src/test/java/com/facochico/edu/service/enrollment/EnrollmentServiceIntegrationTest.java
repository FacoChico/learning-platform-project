package com.facochico.edu.service.enrollment;

import com.facochico.edu.domain.course.dto.CourseDetails;
import com.facochico.edu.domain.course.dto.CourseView;
import com.facochico.edu.domain.course.dto.ModuleDetails;
import com.facochico.edu.domain.course.exception.CourseInUseException;
import com.facochico.edu.domain.enrollment.dto.EnrolledCourse;
import com.facochico.edu.domain.enrollment.dto.EnrolledStudent;
import com.facochico.edu.domain.enrollment.exception.DuplicateEnrollmentException;
import com.facochico.edu.domain.enrollment.exception.EnrollmentNotFoundException;
import com.facochico.edu.domain.user.UserDetails;
import com.facochico.edu.domain.user.exception.InvalidStudentRoleException;
import com.facochico.edu.service.course.CourseService;
import com.facochico.edu.service.user.UserService;
import com.facochico.edu.testutil.WithPostgres;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Executors;
import java.util.stream.Stream;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;

import static com.facochico.edu.domain.enrollment.EnrollmentStatus.ACTIVE;
import static com.facochico.edu.domain.enrollment.EnrollmentStatus.COMPLETED;
import static com.facochico.edu.domain.user.UserRole.STUDENT;
import static com.facochico.edu.domain.user.UserRole.TEACHER;
import static java.util.concurrent.TimeUnit.SECONDS;
import static lombok.AccessLevel.PACKAGE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.catchThrowable;

@SpringBootTest
@WithPostgres
@RequiredArgsConstructor(access = PACKAGE, onConstructor_ = @Autowired)
class EnrollmentServiceIntegrationTest {
    private final EnrollmentService enrollments;
    private final CourseService courses;
    private final UserService users;

    @Test
    void shouldListStudentsAndCoursesWhenEnrollmentsAreSaved() {
        final var teacher = users.create(new UserDetails("Таня", "tanya.enrollment@example.org", TEACHER, null));
        final var java = createCourse("Основы Java", teacher.getId());
        final var sql = createCourse("Практикум по SQL", teacher.getId());
        final var firstStudent = users.create(new UserDetails("Аня", "anya.enrollment@example.org", STUDENT, null));
        final var secondStudent = users.create(new UserDetails("Мишаня", "misha.enrollment@example.org", STUDENT, null));

        final var created = enrollments.enroll(java.id(), firstStudent.getId());
        enrollments.enroll(sql.id(), firstStudent.getId());
        enrollments.enroll(java.id(), secondStudent.getId());

        assertThat(created.status()).isEqualTo(ACTIVE);
        assertThat(created.enrolledAt()).isNotNull();
        assertThat(enrollments.findEnrollment(java.id(), firstStudent.getId())).isEqualTo(created);
        assertThat(enrollments.listStudents(java.id(), 0, 1))
            .containsExactly(new EnrolledStudent(firstStudent.getId(), "Аня", ACTIVE));
        assertThat(enrollments.listStudents(java.id(), 1, 1))
            .containsExactly(new EnrolledStudent(secondStudent.getId(), "Мишаня", ACTIVE));
        assertThat(enrollments.listCourses(firstStudent.getId(), 0, 2))
            .containsExactly(
                new EnrolledCourse(java.id(), "Основы Java", ACTIVE),
                new EnrolledCourse(sql.id(), "Практикум по SQL", ACTIVE)
            );
    }

    @Test
    void shouldPersistCompletedStatusWhenEnrollmentIsCompleted() {
        final var teacher = users.create(new UserDetails("Анна", "anna.complete@example.org", TEACHER, null));
        final var course = createCourse("Структуры данных", teacher.getId());
        final var student = users.create(new UserDetails("Григорий", "grigory.complete@example.org", STUDENT, null));
        final var original = enrollments.enroll(course.id(), student.getId());

        final var completed = enrollments.completeEnrollment(course.id(), student.getId());

        assertThat(completed.status()).isEqualTo(COMPLETED);
        assertThat(completed.enrolledAt()).isEqualTo(original.enrolledAt());
        assertThat(enrollments.findEnrollment(course.id(), student.getId())).isEqualTo(completed);
        assertThat(enrollments.listCourses(student.getId(), 0, 1).getFirst().status()).isEqualTo(COMPLETED);
    }

    @Test
    void shouldRejectEnrollmentWhenStudentIsAlreadyEnrolled() {
        final var teacher = users.create(new UserDetails("Лена", "lena.duplicate@example.org", TEACHER, null));
        final var course = createCourse("Разработка на Spring", teacher.getId());
        final var student = users.create(new UserDetails("Гришаня", "grisha.duplicate@example.org", STUDENT, null));
        final var original = enrollments.enroll(course.id(), student.getId());

        final var failure = catchThrowable(() -> enrollments.enroll(course.id(), student.getId()));

        assertThat(failure).isInstanceOf(DuplicateEnrollmentException.class);
        assertThat(enrollments.findEnrollment(course.id(), student.getId())).isEqualTo(original);
        assertThat(enrollments.listStudents(course.id(), 0, 10)).hasSize(1);
    }

    @Test
    @SneakyThrows
    void shouldKeepSingleEnrollmentWhenTwoRequestsEnrollSameStudent() {
        final var teacher = users.create(new UserDetails("Саша", "sasha.concurrent@example.org", TEACHER, null));
        final var course = createCourse("Многопоточность на Java", teacher.getId());
        final var student = users.create(new UserDetails("Никита", "nikita.concurrent@example.org", STUDENT, null));
        final var ready = new CyclicBarrier(2);
        final Callable<Throwable> request = () -> {
            ready.await(5, SECONDS);
            return catchThrowable(() -> enrollments.enroll(course.id(), student.getId()));
        };

        final Throwable firstFailure;
        final Throwable secondFailure;
        try (final var workers = Executors.newFixedThreadPool(2)) {
            final var first = workers.submit(request);
            final var second = workers.submit(request);
            firstFailure = first.get(10, SECONDS);
            secondFailure = second.get(10, SECONDS);
        }

        final var failures = Stream.of(firstFailure, secondFailure).filter(Objects::nonNull).toList();
        assertThat(failures).singleElement().isInstanceOf(DuplicateEnrollmentException.class);
        assertThat(enrollments.listStudents(course.id(), 0, 10)).hasSize(1);
    }

    @Test
    void shouldRejectEnrollmentWhenUserIsTeacher() {
        final var teacher = users.create(new UserDetails("Сергей", "sergey.role@example.org", TEACHER, null));
        final var course = createCourse("Компьютерные сети", teacher.getId());

        final var failure = catchThrowable(() -> enrollments.enroll(course.id(), teacher.getId()));

        assertThat(failure).isInstanceOf(InvalidStudentRoleException.class);
        assertThat(enrollments.listStudents(course.id(), 0, 10)).isEmpty();
    }

    @Test
    void shouldRemoveEnrollmentWhenStudentWithdraws() {
        final var teacher = users.create(new UserDetails("Илья", "ilya.withdraw@example.org", TEACHER, null));
        final var course = createCourse("Операционные системы", teacher.getId());
        final var student = users.create(new UserDetails("Валя", "valya.withdraw@example.org", STUDENT, null));
        enrollments.enroll(course.id(), student.getId());

        enrollments.withdraw(course.id(), student.getId());

        assertThat(enrollments.listStudents(course.id(), 0, 10)).isEmpty();
        assertThat(enrollments.listCourses(student.getId(), 0, 10)).isEmpty();
        assertThatExceptionOfType(EnrollmentNotFoundException.class)
            .isThrownBy(() -> enrollments.findEnrollment(course.id(), student.getId()));
    }

    @Test
    void shouldCreateNewEnrollmentWhenStudentReturnsAfterWithdrawal() {
        final var teacher = users.create(new UserDetails("Коля", "kolya.return@example.org", TEACHER, null));
        final var course = createCourse("Тестирование приложений", teacher.getId());
        final var student = users.create(new UserDetails("Оля", "olya.return@example.org", STUDENT, null));
        final var original = enrollments.enroll(course.id(), student.getId());
        enrollments.withdraw(course.id(), student.getId());

        final var restored = enrollments.enroll(course.id(), student.getId());

        assertThat(restored.id()).isNotEqualTo(original.id());
        assertThat(restored.status()).isEqualTo(ACTIVE);
        assertThat(enrollments.findEnrollment(course.id(), student.getId())).isEqualTo(restored);
    }

    @Test
    void shouldKeepCourseAndModulesWhenDeletionConflictsWithEnrollment() {
        final var teacher = users.create(new UserDetails("Лиза", "liza.protected@example.org", TEACHER, null));
        final var course = createCourse("Введение в алгоритмы", teacher.getId());
        final var module = courses.addModule(course.id(), new ModuleDetails("Сортировки", null, 0));
        final var student = users.create(new UserDetails("Артур", "artur.protected@example.org", STUDENT, null));
        final var enrollment = enrollments.enroll(course.id(), student.getId());

        final var failure = catchThrowable(() -> courses.deleteCourse(course.id()));

        assertThat(failure).isInstanceOf(CourseInUseException.class);
        assertThat(courses.findCourseById(course.id()).modules()).containsExactly(module);
        assertThat(enrollments.findEnrollment(course.id(), student.getId())).isEqualTo(enrollment);
    }

    private CourseView createCourse(String title, long teacherId) {
        final var category = courses.createCategory("Информатика");
        return courses.createCourse(new CourseDetails(title, null, category.id(), teacherId, Set.of()));
    }
}
