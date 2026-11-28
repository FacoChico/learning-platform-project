package com.facochico.edu.controller.course;

import com.facochico.edu.domain.course.dto.LessonDetails;
import com.facochico.edu.domain.course.dto.LessonView;
import com.facochico.edu.domain.course.dto.ModuleDetails;
import com.facochico.edu.domain.course.dto.ModuleView;
import com.facochico.edu.service.course.CourseService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

import jakarta.validation.constraints.Positive;

import lombok.RequiredArgsConstructor;

import static org.springframework.http.HttpStatus.NO_CONTENT;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
class CurriculumController {
    private final CourseService courses;

    @PostMapping("/courses/{courseId}/modules")
    ResponseEntity<ModuleView> addModule(@PathVariable @Positive long courseId, @RequestBody ModuleDetails details) {
        final var module = courses.addModule(courseId, details);
        return ResponseEntity.created(URI.create("/api/v1/modules/" + module.id())).body(module);
    }

    @GetMapping("/modules/{id}")
    ModuleView findModule(@PathVariable @Positive long id) {
        return courses.findModuleById(id);
    }

    @PutMapping("/modules/{id}")
    ModuleView updateModule(@PathVariable @Positive long id, @RequestBody ModuleDetails details) {
        return courses.updateModule(id, details);
    }

    @DeleteMapping("/modules/{id}")
    @ResponseStatus(NO_CONTENT)
    void deleteModule(@PathVariable @Positive long id) {
        courses.deleteModule(id);
    }

    @PostMapping("/modules/{moduleId}/lessons")
    ResponseEntity<LessonView> addLesson(@PathVariable @Positive long moduleId, @RequestBody LessonDetails details) {
        final var lesson = courses.addLesson(moduleId, details);
        return ResponseEntity.created(URI.create("/api/v1/lessons/" + lesson.id())).body(lesson);
    }

    @GetMapping("/lessons/{id}")
    LessonView findLesson(@PathVariable @Positive long id) {
        return courses.findLessonById(id);
    }

    @PutMapping("/lessons/{id}")
    LessonView updateLesson(@PathVariable @Positive long id, @RequestBody LessonDetails details) {
        return courses.updateLesson(id, details);
    }

    @DeleteMapping("/lessons/{id}")
    @ResponseStatus(NO_CONTENT)
    void deleteLesson(@PathVariable @Positive long id) {
        courses.deleteLesson(id);
    }
}
