package com.facochico.edu.repository.course;

import com.facochico.edu.domain.course.dto.CourseDetails;
import com.facochico.edu.domain.course.dto.ModuleDetails;
import com.facochico.edu.domain.user.UserDetails;
import com.facochico.edu.service.course.CourseService;
import com.facochico.edu.service.user.UserService;
import com.facochico.edu.testutil.WithPostgres;

import org.hibernate.LazyInitializationException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Set;

import lombok.RequiredArgsConstructor;

import static com.facochico.edu.domain.user.UserRole.TEACHER;
import static lombok.AccessLevel.PACKAGE;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

@SpringBootTest
@WithPostgres
@RequiredArgsConstructor(access = PACKAGE, onConstructor_ = @Autowired)
class SpringDataCourseRepositoryTest {
    private final SpringDataCourseRepository repository;
    private final CourseService courses;
    private final UserService users;

    @Test
    void shouldThrowLazyInitializationExceptionWhenDetachedCourseModulesAreRead() {
        final var teacher = users.create(new UserDetails("Мишаня", "misha.lazy@example.org", TEACHER, null));
        final var category = courses.createCategory("Хранение данных");
        final var course = courses.createCourse(new CourseDetails("Связи в JPA", null, category.id(), teacher.getId(), Set.of()));
        courses.addModule(course.id(), new ModuleDetails("Ленивая загрузка", null, 0));

        final var detached = repository.findById(course.id()).orElseThrow();

        assertThatExceptionOfType(LazyInitializationException.class).isThrownBy(() -> detached.getModules().size());
    }
}
