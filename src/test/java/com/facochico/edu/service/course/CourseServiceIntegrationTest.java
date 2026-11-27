package com.facochico.edu.service.course;

import com.facochico.edu.domain.course.dto.CourseDetails;
import com.facochico.edu.domain.course.dto.LessonDetails;
import com.facochico.edu.domain.course.dto.LessonView;
import com.facochico.edu.domain.course.dto.ModuleDetails;
import com.facochico.edu.domain.course.dto.ModuleView;
import com.facochico.edu.domain.course.exception.CatalogEntryInUseException;
import com.facochico.edu.domain.course.exception.CurriculumItemNotFoundException;
import com.facochico.edu.domain.course.exception.InvalidCourseTeacherException;
import com.facochico.edu.domain.user.UserDetails;
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
class CourseServiceIntegrationTest {
    private final CourseService courses;
    private final UserService users;

    @Test
    void shouldReturnOrderedCurriculumWhenModulesAndLessonsAreCreatedOutOfOrder() {
        final var details = courseDetails("Основы ORM", "Саша", "sasha.orm@example.org");
        final var course = courses.createCourse(details);
        final var workshop = courses.addModule(course.id(), new ModuleDetails("Практика", null, 2));
        final var introduction = courses.addModule(course.id(), new ModuleDetails("Введение", null, 1));
        final var queries = courses.addLesson(introduction.id(), new LessonDetails("Запросы", null, null, 2));
        final var entities = courses.addLesson(introduction.id(), new LessonDetails("Сущности", null, null, 1));
        final var session = courses.addLesson(introduction.id(), new LessonDetails("Сессия", null, null, 2));

        final var stored = courses.findCourseById(course.id());

        assertThat(stored.teacherId()).isEqualTo(details.teacherId());
        assertThat(stored.category().id()).isEqualTo(details.categoryId());
        assertThat(stored.tags()).hasSize(1);
        assertThat(stored.modules()).extracting(ModuleView::id).containsExactly(introduction.id(), workshop.id());
        assertThat(stored.modules().getFirst().lessons()).extracting(LessonView::id)
            .containsExactly(entities.id(), queries.id(), session.id());
        assertThat(stored.modules().getLast().lessons()).isEmpty();
    }

    @Test
    void shouldStoreReassignedTeacherCategoryAndTagsWhenCourseIsUpdated() {
        final var original = courses.createCourse(courseDetails("Введение в SQL", "Таня", "tanya.sql@example.org"));
        final var teacher = users.create(new UserDetails("Анна", "anna.sql@example.org", TEACHER, null));
        final var category = courses.createCategory("Реляционные базы данных");
        final var tag = courses.createTag("Запросы");
        final var updated = new CourseDetails(
            "Практикум по SQL", "Разбор задач с семинаров", category.id(), teacher.getId(), Set.of(tag.id())
        );

        courses.updateCourse(original.id(), updated);

        final var stored = courses.findCourseById(original.id());
        assertThat(stored.title()).isEqualTo("Практикум по SQL");
        assertThat(stored.description()).isEqualTo("Разбор задач с семинаров");
        assertThat(stored.teacherId()).isEqualTo(teacher.getId());
        assertThat(stored.category()).isEqualTo(category);
        assertThat(stored.tags()).containsExactly(tag);
        assertThat(courses.findTagById(original.tags().getFirst().id())).isEqualTo(original.tags().getFirst());
    }

    @Test
    void shouldReturnConsecutivePagesWhenCourseListIsLimited() {
        courses.createCourse(courseDetails("Разработка на Java", "Михаил", "mikhail.java@example.org"));
        courses.createCourse(courseDetails("Проектирование БД", "Лена", "lena.db@example.org"));

        final var first = courses.listCourses(0, 1);
        final var second = courses.listCourses(1, 1);

        assertThat(first).hasSize(1);
        assertThat(second).hasSize(1);
        assertThat(first.getFirst().id()).isLessThan(second.getFirst().id());
    }

    @Test
    void shouldRejectCourseWhenAssignedUserIsStudent() {
        final var student = users.create(new UserDetails("Гришаня", "grisha.course@example.org", STUDENT, null));
        final var category = courses.createCategory("Программирование");
        final var details = new CourseDetails("Основы Java", null, category.id(), student.getId(), Set.of());

        final var failure = catchThrowable(() -> courses.createCourse(details));

        assertThat(failure).isInstanceOf(InvalidCourseTeacherException.class)
            .hasMessage("Course teacher must have the TEACHER role");
    }

    @Test
    void shouldKeepCourseWhenUpdateRefersToMissingTag() {
        final var details = courseDetails("История вычислений", "Ира", "ira.history@example.org");
        final var original = courses.createCourse(details);
        final var update = new CourseDetails(
            "Новый курс", null, details.categoryId(), details.teacherId(), Set.of(Long.MAX_VALUE)
        );

        final var failure = catchThrowable(() -> courses.updateCourse(original.id(), update));

        assertThat(failure).isInstanceOf(CurriculumItemNotFoundException.class)
            .hasMessage("Tag %d was not found".formatted(Long.MAX_VALUE));
        assertThat(courses.findCourseById(original.id())).isEqualTo(original);
    }

    @Test
    void shouldStoreModuleAndLessonUpdatesWhenCurriculumIsEdited() {
        final var course = courses.createCourse(courseDetails("Алгоритмы", "Аня", "anya.algorithms@example.org"));
        final var module = courses.addModule(course.id(), new ModuleDetails("Первый модуль", null, 0));
        final var lesson = courses.addLesson(module.id(), new LessonDetails("Первый урок", null, null, 0));
        final var moduleDetails = new ModuleDetails("Структуры данных", "Списки и очереди", 1);
        final var lessonDetails = new LessonDetails("Очереди", "Разбор примера", "https://example.org/queue", 2);

        courses.updateModule(module.id(), moduleDetails);
        courses.updateLesson(lesson.id(), lessonDetails);

        final var storedLesson = courses.findLessonById(lesson.id());
        final var storedModule = courses.findModuleById(module.id());
        assertThat(storedLesson.details()).isEqualTo(lessonDetails);
        assertThat(storedModule.details()).isEqualTo(moduleDetails);
        assertThat(storedModule.lessons()).containsExactly(storedLesson);
    }

    @Test
    void shouldKeepModuleWhenOneLessonIsRemoved() {
        final var course = courses.createCourse(courseDetails("Сети", "Сергей", "sergey.networks@example.org"));
        final var module = courses.addModule(course.id(), new ModuleDetails("Транспортный уровень", null, 0));
        final var removed = courses.addLesson(module.id(), new LessonDetails("UDP", null, null, 0));
        final var retained = courses.addLesson(module.id(), new LessonDetails("TCP", null, null, 1));

        courses.deleteLesson(removed.id());

        assertThat(courses.findModuleById(module.id()).lessons()).containsExactly(retained);
        assertThatExceptionOfType(CurriculumItemNotFoundException.class)
            .isThrownBy(() -> courses.findLessonById(removed.id()));
    }

    @Test
    void shouldDeleteLessonsWhenModuleIsRemoved() {
        final var course = courses.createCourse(courseDetails("Веб-разработка", "Оля", "olya.web@example.org"));
        final var module = courses.addModule(course.id(), new ModuleDetails("Основы HTTP", null, 0));
        final var lesson = courses.addLesson(module.id(), new LessonDetails("Методы запроса", null, null, 0));

        courses.deleteModule(module.id());

        assertThat(courses.findCourseById(course.id()).modules()).isEmpty();
        assertThatExceptionOfType(CurriculumItemNotFoundException.class)
            .isThrownBy(() -> courses.findLessonById(lesson.id()));
    }

    @Test
    void shouldDeleteOwnedContentWhenCourseIsRemoved() {
        final var details = courseDetails("Тестирование", "Коля", "kolya.tests@example.org");
        final var course = courses.createCourse(details);
        final var related = courses.createCourse(new CourseDetails(
            "Проверка API", null, details.categoryId(), details.teacherId(), details.tagIds()
        ));
        final var module = courses.addModule(course.id(), new ModuleDetails("Интеграционные тесты", null, 0));
        final var lesson = courses.addLesson(module.id(), new LessonDetails("Тестовая база", null, null, 0));

        courses.deleteCourse(course.id());

        assertThatExceptionOfType(CurriculumItemNotFoundException.class)
            .isThrownBy(() -> courses.findCourseById(course.id()));
        assertThatExceptionOfType(CurriculumItemNotFoundException.class)
            .isThrownBy(() -> courses.findModuleById(module.id()));
        assertThatExceptionOfType(CurriculumItemNotFoundException.class)
            .isThrownBy(() -> courses.findLessonById(lesson.id()));
        assertThat(users.findById(course.teacherId()).getId()).isEqualTo(course.teacherId());
        assertThat(courses.findCategoryById(course.category().id())).isEqualTo(course.category());
        assertThat(courses.findTagById(course.tags().getFirst().id())).isEqualTo(course.tags().getFirst());
        assertThat(courses.findCourseById(related.id()).tags()).containsExactlyElementsOf(course.tags());
    }

    @Test
    void shouldReadRenamedCatalogEntriesWhenUpdatesAreSaved() {
        final var category = courses.createCategory("Теория");
        final var tag = courses.createTag("Конспекты");

        courses.updateCategory(category.id(), "Теоретические основы");
        courses.updateTag(tag.id(), "Материалы лекций");

        assertThat(courses.findCategoryById(category.id()).name()).isEqualTo("Теоретические основы");
        assertThat(courses.findTagById(tag.id()).name()).isEqualTo("Материалы лекций");
    }

    @Test
    void shouldDeleteCatalogEntriesWhenNoCourseUsesThem() {
        final var category = courses.createCategory("Факультативы");
        final var tag = courses.createTag("Дополнительное чтение");

        courses.deleteCategory(category.id());
        courses.deleteTag(tag.id());

        assertThatExceptionOfType(CurriculumItemNotFoundException.class)
            .isThrownBy(() -> courses.findCategoryById(category.id()));
        assertThatExceptionOfType(CurriculumItemNotFoundException.class)
            .isThrownBy(() -> courses.findTagById(tag.id()));
    }

    @Test
    void shouldRejectDeletionWhenCourseUsesCategoryAndTag() {
        final var course = courses.createCourse(courseDetails("Операционные системы", "Илья", "ilya.os@example.org"));
        final var categoryId = course.category().id();
        final var tagId = course.tags().getFirst().id();

        final var categoryFailure = catchThrowable(() -> courses.deleteCategory(categoryId));
        final var tagFailure = catchThrowable(() -> courses.deleteTag(tagId));

        assertThat(categoryFailure).isInstanceOf(CatalogEntryInUseException.class);
        assertThat(tagFailure).isInstanceOf(CatalogEntryInUseException.class);
        assertThat(courses.findCourseById(course.id())).isEqualTo(course);
    }

    private CourseDetails courseDetails(String title, String teacherName, String email) {
        final var teacher = users.create(new UserDetails(teacherName, email, TEACHER, null));
        final var category = courses.createCategory("Информатика");
        final var tag = courses.createTag("Практика");
        return new CourseDetails(title, null, category.id(), teacher.getId(), Set.of(tag.id()));
    }
}
