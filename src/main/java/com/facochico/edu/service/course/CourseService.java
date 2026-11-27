package com.facochico.edu.service.course;

import com.facochico.edu.domain.course.dto.CatalogEntry;
import com.facochico.edu.domain.course.dto.CourseDetails;
import com.facochico.edu.domain.course.dto.CourseSummary;
import com.facochico.edu.domain.course.dto.CourseView;
import com.facochico.edu.domain.course.dto.LessonDetails;
import com.facochico.edu.domain.course.dto.LessonView;
import com.facochico.edu.domain.course.dto.ModuleDetails;
import com.facochico.edu.domain.course.dto.ModuleView;
import com.facochico.edu.domain.course.exception.CurriculumItemNotFoundException;
import com.facochico.edu.repository.course.CourseRepository;
import com.facochico.edu.util.course.CurriculumValidationUtil;

import java.util.List;
import java.util.Optional;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class CourseService {
    private final CourseRepository courses;

    public CourseView createCourse(CourseDetails details) {
        return courses.insertCourse(details);
    }

    public CourseView findCourseById(long id) {
        return requireFound(courses.findCourseById(id), "Course", id);
    }

    public List<CourseSummary> listCourses(int page, int size) {
        CurriculumValidationUtil.requirePage(page, size);
        return courses.listCourses(page, size);
    }

    public CourseView updateCourse(long id, CourseDetails details) {
        return requireFound(courses.updateCourse(id, details), "Course", id);
    }

    public void deleteCourse(long id) {
        requireDeleted(courses.deleteCourse(id), "Course", id);
    }

    public ModuleView addModule(long courseId, ModuleDetails details) {
        return courses.insertModule(courseId, details);
    }

    public ModuleView findModuleById(long id) {
        return requireFound(courses.findModuleById(id), "Module", id);
    }

    public ModuleView updateModule(long id, ModuleDetails details) {
        return requireFound(courses.updateModule(id, details), "Module", id);
    }

    public void deleteModule(long id) {
        requireDeleted(courses.deleteModule(id), "Module", id);
    }

    public LessonView addLesson(long moduleId, LessonDetails details) {
        return courses.insertLesson(moduleId, details);
    }

    public LessonView findLessonById(long id) {
        return requireFound(courses.findLessonById(id), "Lesson", id);
    }

    public LessonView updateLesson(long id, LessonDetails details) {
        return requireFound(courses.updateLesson(id, details), "Lesson", id);
    }

    public void deleteLesson(long id) {
        requireDeleted(courses.deleteLesson(id), "Lesson", id);
    }

    public CatalogEntry createCategory(String name) {
        final var normalized = CurriculumValidationUtil.requireLabel(name, "Category name");
        return courses.insertCategory(normalized);
    }

    public CatalogEntry findCategoryById(long id) {
        return requireFound(courses.findCategoryById(id), "Category", id);
    }

    public CatalogEntry updateCategory(long id, String name) {
        final var normalized = CurriculumValidationUtil.requireLabel(name, "Category name");
        return requireFound(courses.updateCategory(id, normalized), "Category", id);
    }

    public void deleteCategory(long id) {
        requireDeleted(courses.deleteCategory(id), "Category", id);
    }

    public CatalogEntry createTag(String name) {
        final var normalized = CurriculumValidationUtil.requireLabel(name, "Tag name");
        return courses.insertTag(normalized);
    }

    public CatalogEntry findTagById(long id) {
        return requireFound(courses.findTagById(id), "Tag", id);
    }

    public CatalogEntry updateTag(long id, String name) {
        final var normalized = CurriculumValidationUtil.requireLabel(name, "Tag name");
        return requireFound(courses.updateTag(id, normalized), "Tag", id);
    }

    public void deleteTag(long id) {
        requireDeleted(courses.deleteTag(id), "Tag", id);
    }

    private static <T> T requireFound(Optional<T> result, String resource, long id) {
        return result.orElseThrow(() -> new CurriculumItemNotFoundException(resource, id));
    }

    private static void requireDeleted(boolean deleted, String resource, long id) {
        if (!deleted) {
            throw new CurriculumItemNotFoundException(resource, id);
        }
    }
}
