package com.facochico.edu.repository.course;

import com.facochico.edu.domain.course.dto.CatalogEntry;
import com.facochico.edu.domain.course.dto.CourseDetails;
import com.facochico.edu.domain.course.dto.CourseSummary;
import com.facochico.edu.domain.course.dto.CourseView;
import com.facochico.edu.domain.course.dto.LessonDetails;
import com.facochico.edu.domain.course.dto.LessonView;
import com.facochico.edu.domain.course.dto.ModuleDetails;
import com.facochico.edu.domain.course.dto.ModuleView;

import java.util.List;
import java.util.Optional;

public interface CourseRepository {

    CourseView insertCourse(CourseDetails details);

    Optional<CourseView> findCourseById(long id);

    List<CourseSummary> listCourses(int page, int size);

    Optional<CourseView> updateCourse(long id, CourseDetails details);

    boolean deleteCourse(long id);

    ModuleView insertModule(long courseId, ModuleDetails details);

    Optional<ModuleView> findModuleById(long id);

    Optional<ModuleView> updateModule(long id, ModuleDetails details);

    boolean deleteModule(long id);

    LessonView insertLesson(long moduleId, LessonDetails details);

    Optional<LessonView> findLessonById(long id);

    Optional<LessonView> updateLesson(long id, LessonDetails details);

    boolean deleteLesson(long id);

    CatalogEntry insertCategory(String name);

    Optional<CatalogEntry> findCategoryById(long id);

    Optional<CatalogEntry> updateCategory(long id, String name);

    boolean deleteCategory(long id);

    CatalogEntry insertTag(String name);

    Optional<CatalogEntry> findTagById(long id);

    Optional<CatalogEntry> updateTag(long id, String name);

    boolean deleteTag(long id);
}
