package com.facochico.edu.repository.course;

import com.facochico.edu.domain.course.CategoryEntity;
import com.facochico.edu.domain.course.CourseEntity;
import com.facochico.edu.domain.course.LessonEntity;
import com.facochico.edu.domain.course.ModuleEntity;
import com.facochico.edu.domain.course.TagEntity;
import com.facochico.edu.domain.course.dto.CatalogEntry;
import com.facochico.edu.domain.course.dto.CourseDetails;
import com.facochico.edu.domain.course.dto.CourseSummary;
import com.facochico.edu.domain.course.dto.CourseView;
import com.facochico.edu.domain.course.dto.LessonDetails;
import com.facochico.edu.domain.course.dto.LessonView;
import com.facochico.edu.domain.course.dto.ModuleDetails;
import com.facochico.edu.domain.course.dto.ModuleView;
import com.facochico.edu.domain.course.exception.CatalogEntryInUseException;
import com.facochico.edu.domain.course.exception.CourseInUseException;
import com.facochico.edu.domain.course.exception.CurriculumItemNotFoundException;
import com.facochico.edu.domain.homework.exception.HomeworkInUseException;
import com.facochico.edu.domain.user.UserEntity;
import com.facochico.edu.domain.user.exception.UserNotFoundException;
import com.facochico.edu.util.persistence.ConstraintViolationUtil;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import jakarta.persistence.EntityManager;

import lombok.RequiredArgsConstructor;

import static com.facochico.edu.domain.course.CourseEntity.CATEGORY_FOREIGN_KEY;
import static com.facochico.edu.domain.course.CourseEntity.TAG_FOREIGN_KEY;
import static com.facochico.edu.domain.enrollment.EnrollmentEntity.COURSE_FOREIGN_KEY;
import static com.facochico.edu.domain.homework.SubmissionEntity.ASSIGNMENT_FOREIGN_KEY;
import static java.util.Comparator.comparingLong;
import static java.util.stream.Collectors.toSet;
import static lombok.AccessLevel.PACKAGE;

@Repository
@Transactional
@RequiredArgsConstructor(access = PACKAGE)
class JpaCourseRepository implements CourseRepository {
    private final SpringDataCourseRepository courses;
    private final SpringDataModuleRepository modules;
    private final SpringDataLessonRepository lessons;
    private final SpringDataCategoryRepository categories;
    private final SpringDataTagRepository tags;
    private final EntityManager entityManager;

    @Override
    public CourseView insertCourse(CourseDetails details) {
        final var category = requireCategory(details.categoryId());
        final var teacher = requireUser(details.teacherId());
        final var selectedTags = loadTags(details.tagIds());
        final var course = courses.save(new CourseEntity(details, category, teacher, selectedTags));

        return toCourseView(course, List.of());
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<CourseView> findCourseById(long id) {
        return courses.findWithCatalogById(id)
            .map(this::loadCourseView);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CourseSummary> listCourses(int page, int size) {
        return courses.findSummaries(PageRequest.of(page, size));
    }

    @Override
    public Optional<CourseView> updateCourse(long id, CourseDetails details) {
        return courses.findWithCatalogById(id)
            .map(course -> {
                final var category = requireCategory(details.categoryId());
                final var teacher = requireUser(details.teacherId());
                final var selectedTags = loadTags(details.tagIds());
                course.update(details, category, teacher, selectedTags);

                return loadCourseView(course);
            });
    }

    @Override
    public boolean deleteCourse(long id) {
        try {
            return deleteAndFlushIfPresent(courses, id);
        } catch (DataIntegrityViolationException exception) {
            if (ConstraintViolationUtil.violatesConstraint(exception, COURSE_FOREIGN_KEY)) {
                throw new CourseInUseException(id, exception);
            }
            throw translateHomeworkDeletion(exception, "Course", id);
        }
    }

    @Override
    public ModuleView insertModule(long courseId, ModuleDetails details) {
        final var course = courses.findById(courseId)
            .orElseThrow(() -> new CurriculumItemNotFoundException("Course", courseId));
        final var module = modules.save(course.addModule(details));

        return toModuleView(module);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ModuleView> findModuleById(long id) {
        return modules.findWithLessonsById(id)
            .map(JpaCourseRepository::toModuleView);
    }

    @Override
    public Optional<ModuleView> updateModule(long id, ModuleDetails details) {
        return modules.findWithLessonsById(id)
            .map(module -> {
                module.update(details);
                return toModuleView(module);
            });
    }

    @Override
    public boolean deleteModule(long id) {
        final var module = modules.findById(id);
        if (module.isEmpty()) {
            return false;
        }

        final var removed = module.get();
        removed.getCourse().removeModule(removed);
        flushCurriculumDeletion(modules, "Module", id);
        return true;
    }

    @Override
    public LessonView insertLesson(long moduleId, LessonDetails details) {
        final var module = modules.findById(moduleId)
            .orElseThrow(() -> new CurriculumItemNotFoundException("Module", moduleId));
        final var lesson = lessons.save(module.addLesson(details));

        return toLessonView(lesson);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<LessonView> findLessonById(long id) {
        return lessons.findById(id)
            .map(JpaCourseRepository::toLessonView);
    }

    @Override
    public Optional<LessonView> updateLesson(long id, LessonDetails details) {
        return lessons.findById(id)
            .map(lesson -> {
                lesson.update(details);
                return toLessonView(lesson);
            });
    }

    @Override
    public boolean deleteLesson(long id) {
        final var lesson = lessons.findById(id);
        if (lesson.isEmpty()) {
            return false;
        }

        final var removed = lesson.get();
        removed.getModule().removeLesson(removed);
        flushCurriculumDeletion(lessons, "Lesson", id);
        return true;
    }

    @Override
    public CatalogEntry insertCategory(String name) {
        final var category = categories.save(new CategoryEntity(name));
        return new CatalogEntry(category.getId(), category.getName());
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<CatalogEntry> findCategoryById(long id) {
        return categories.findById(id)
            .map(category -> new CatalogEntry(category.getId(), category.getName()));
    }

    @Override
    public Optional<CatalogEntry> updateCategory(long id, String name) {
        return categories.findById(id)
            .map(category -> {
                category.setName(name);
                return new CatalogEntry(category.getId(), category.getName());
            });
    }

    @Override
    public boolean deleteCategory(long id) {
        return deleteCatalogEntry(categories, id, "Category", CATEGORY_FOREIGN_KEY);
    }

    @Override
    public CatalogEntry insertTag(String name) {
        final var tag = tags.save(new TagEntity(name));
        return new CatalogEntry(tag.getId(), tag.getName());
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<CatalogEntry> findTagById(long id) {
        return tags.findById(id)
            .map(tag -> new CatalogEntry(tag.getId(), tag.getName()));
    }

    @Override
    public Optional<CatalogEntry> updateTag(long id, String name) {
        return tags.findById(id)
            .map(tag -> {
                tag.setName(name);
                return new CatalogEntry(tag.getId(), tag.getName());
            });
    }

    @Override
    public boolean deleteTag(long id) {
        return deleteCatalogEntry(tags, id, "Tag", TAG_FOREIGN_KEY);
    }

    private CategoryEntity requireCategory(long id) {
        return categories.findById(id)
            .orElseThrow(() -> new CurriculumItemNotFoundException("Category", id));
    }

    private UserEntity requireUser(long id) {
        final var teacher = entityManager.find(UserEntity.class, id);
        if (teacher == null) {
            throw new UserNotFoundException(id);
        }
        return teacher;
    }

    private List<TagEntity> loadTags(Set<Long> ids) {
        if (ids.isEmpty()) {
            return List.of();
        }

        final var selected = tags.findAllById(ids);
        if (selected.size() != ids.size()) {
            final var found = selected.stream()
                .map(TagEntity::getId)
                .collect(toSet());
            final var missing = ids.stream()
                .filter(id -> !found.contains(id))
                .min(Long::compareTo)
                .orElseThrow();
            throw new CurriculumItemNotFoundException("Tag", missing);
        }
        return selected;
    }

    private CourseView loadCourseView(CourseEntity course) {
        final var curriculum = modules.findWithLessonsByCourseId(course.getId()).stream()
            .map(JpaCourseRepository::toModuleView)
            .toList();
        return toCourseView(course, curriculum);
    }

    private static CourseView toCourseView(CourseEntity course, List<ModuleView> curriculum) {
        final var category = course.getCategory();
        final var courseTags = course.getTags().stream()
            .map(tag -> new CatalogEntry(tag.getId(), tag.getName()))
            .sorted(comparingLong(CatalogEntry::id))
            .toList();

        return new CourseView(
            course.getId(), course.getTitle(), course.getDescription(), course.getTeacher().getId(),
            new CatalogEntry(category.getId(), category.getName()), courseTags, curriculum
        );
    }

    private static ModuleView toModuleView(ModuleEntity module) {
        final var details = new ModuleDetails(module.getTitle(), module.getDescription(), module.getOrderIndex());
        final var contents = module.getLessons().stream()
            .map(JpaCourseRepository::toLessonView)
            .toList();
        return new ModuleView(module.getId(), module.getCourse().getId(), details, contents);
    }

    private static LessonView toLessonView(LessonEntity lesson) {
        final var details = new LessonDetails(
            lesson.getTitle(), lesson.getContent(), lesson.getVideoUrl(), lesson.getOrderIndex()
        );
        return new LessonView(lesson.getId(), lesson.getModule().getId(), details);
    }

    private static <T> boolean deleteAndFlushIfPresent(JpaRepository<T, Long> repository, long id) {
        final var entity = repository.findById(id);
        if (entity.isEmpty()) {
            return false;
        }

        repository.delete(entity.get());
        repository.flush();
        return true;
    }

    private static <T> boolean deleteCatalogEntry(
        JpaRepository<T, Long> repository, long id, String resource, String foreignKey
    ) {
        try {
            return deleteAndFlushIfPresent(repository, id);
        } catch (DataIntegrityViolationException exception) {
            if (ConstraintViolationUtil.violatesConstraint(exception, foreignKey)) {
                throw new CatalogEntryInUseException(resource, id, exception);
            }
            throw exception;
        }
    }

    private static void flushCurriculumDeletion(JpaRepository<?, Long> repository, String resource, long id) {
        try {
            repository.flush();
        } catch (DataIntegrityViolationException exception) {
            throw translateHomeworkDeletion(exception, resource, id);
        }
    }

    private static RuntimeException translateHomeworkDeletion(
        DataIntegrityViolationException exception, String resource, long id
    ) {
        if (ConstraintViolationUtil.violatesConstraint(exception, ASSIGNMENT_FOREIGN_KEY)) {
            return new HomeworkInUseException(resource, id, exception);
        }
        return exception;
    }
}
