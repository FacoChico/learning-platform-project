package com.facochico.edu.service.course;

import com.facochico.edu.domain.course.dto.CourseDetails;
import com.facochico.edu.domain.course.dto.CourseReviewDetails;
import com.facochico.edu.domain.course.dto.CourseView;
import com.facochico.edu.domain.course.exception.CourseReviewNotFoundException;
import com.facochico.edu.domain.user.UserDetails;
import com.facochico.edu.domain.user.exception.InvalidStudentRoleException;
import com.facochico.edu.service.user.UserService;
import com.facochico.edu.testutil.WithPostgres;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Set;

import lombok.RequiredArgsConstructor;

import static com.facochico.edu.domain.user.UserRole.STUDENT;
import static com.facochico.edu.domain.user.UserRole.TEACHER;
import static lombok.AccessLevel.PACKAGE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.catchThrowable;

@SpringBootTest
@WithPostgres
@RequiredArgsConstructor(access = PACKAGE, onConstructor_ = @Autowired)
class CourseReviewServiceIntegrationTest {
    private final CourseReviewService reviews;
    private final CourseService courses;
    private final UserService users;

    @Test
    void shouldListUpdatedReviewWhenStudentChangesRatingAndComment() {
        final var course = createCourse("Разбор задач на Java", "anna.review-update@example.org");
        final var student = users.create(new UserDetails("Аня", "anya.review-update@example.org", STUDENT, null));
        final var original = reviews.createReview(course.id(), student.getId(), new CourseReviewDetails(4, "Понятная теория"));
        final var details = new CourseReviewDetails(5, "Примеры с практики пригодились на зачёте");

        final var updated = reviews.updateReview(original.id(), details);

        assertThat(updated.studentId()).isEqualTo(student.getId());
        assertThat(updated.createdAt()).isEqualTo(original.createdAt());
        assertThat(updated.details()).isEqualTo(details);
        assertThat(reviews.findReviewById(original.id())).isEqualTo(updated);
        assertThat(reviews.listReviews(course.id(), 0, 10)).containsExactly(updated);
    }

    @Test
    void shouldRemoveReviewWhenItIsDeleted() {
        final var course = createCourse("Практика SQL", "anna.review-delete@example.org");
        final var student = users.create(new UserDetails("Григорий", "grigory.review-delete@example.org", STUDENT, null));
        final var review = reviews.createReview(course.id(), student.getId(), new CourseReviewDetails(3, null));

        reviews.deleteReview(review.id());

        assertThat(reviews.listReviews(course.id(), 0, 10)).isEmpty();
        assertThatExceptionOfType(CourseReviewNotFoundException.class).isThrownBy(() -> reviews.findReviewById(review.id()));
    }

    @Test
    void shouldRejectReviewWhenAuthorIsTeacher() {
        final var course = createCourse("Хранение данных", "anna.review-role@example.org");
        final var details = new CourseReviewDetails(5, "Хороший практикум");

        final var failure = catchThrowable(() -> reviews.createReview(course.id(), course.teacherId(), details));

        assertThat(failure).isInstanceOf(InvalidStudentRoleException.class);
        assertThat(reviews.listReviews(course.id(), 0, 10)).isEmpty();
    }

    @Test
    void shouldRemoveReviewWhenItsCourseIsDeleted() {
        final var course = createCourse("ООП на Java", "anna.review-cascade@example.org");
        final var student = users.create(new UserDetails("Мишаня", "misha.review-cascade@example.org", STUDENT, null));
        final var review = reviews.createReview(
            course.id(), student.getId(), new CourseReviewDetails(1, "Не хватило примеров")
        );

        courses.deleteCourse(course.id());

        assertThatExceptionOfType(CourseReviewNotFoundException.class).isThrownBy(() -> reviews.findReviewById(review.id()));
        assertThat(users.findById(student.getId())).isEqualTo(student);
    }

    private CourseView createCourse(String title, String teacherEmail) {
        final var teacher = users.create(new UserDetails("Анна", teacherEmail, TEACHER, null));
        final var category = courses.createCategory("Базы данных и программирование");
        return courses.createCourse(new CourseDetails(title, null, category.id(), teacher.getId(), Set.of()));
    }
}
