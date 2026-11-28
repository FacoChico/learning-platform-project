package com.facochico.edu.controller.course;

import com.facochico.edu.controller.dto.PageQuery;
import com.facochico.edu.domain.course.dto.CourseDetails;
import com.facochico.edu.domain.course.dto.CourseSummary;
import com.facochico.edu.domain.course.dto.CourseView;
import com.facochico.edu.service.course.CourseService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;

import lombok.RequiredArgsConstructor;

import static org.springframework.http.HttpStatus.NO_CONTENT;

@RestController
@RequestMapping("/api/v1/courses")
@RequiredArgsConstructor
class CourseController {
    private final CourseService courses;

    @PostMapping
    ResponseEntity<CourseView> create(@RequestBody CourseDetails details) {
        final var course = courses.createCourse(details);
        return ResponseEntity.created(URI.create("/api/v1/courses/" + course.id())).body(course);
    }

    @GetMapping
    List<CourseSummary> list(@ModelAttribute @Valid PageQuery page) {
        return courses.listCourses(page.page(), page.size());
    }

    @GetMapping("/{id}")
    CourseView findById(@PathVariable @Positive long id) {
        return courses.findCourseById(id);
    }

    @PutMapping("/{id}")
    CourseView update(@PathVariable @Positive long id, @RequestBody CourseDetails details) {
        return courses.updateCourse(id, details);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(NO_CONTENT)
    void delete(@PathVariable @Positive long id) {
        courses.deleteCourse(id);
    }
}
