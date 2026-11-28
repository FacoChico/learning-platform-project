package com.facochico.edu.controller.course;

import com.facochico.edu.controller.course.dto.NameRequest;
import com.facochico.edu.domain.course.dto.CatalogEntry;
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

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;

import lombok.RequiredArgsConstructor;

import static org.springframework.http.HttpStatus.NO_CONTENT;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
class CatalogController {
    private final CourseService courses;

    @PostMapping("/categories")
    ResponseEntity<CatalogEntry> createCategory(@RequestBody @Valid NameRequest request) {
        final var category = courses.createCategory(request.name());
        return ResponseEntity.created(URI.create("/api/v1/categories/" + category.id())).body(category);
    }

    @GetMapping("/categories/{id}")
    CatalogEntry findCategory(@PathVariable @Positive long id) {
        return courses.findCategoryById(id);
    }

    @PutMapping("/categories/{id}")
    CatalogEntry updateCategory(@PathVariable @Positive long id, @RequestBody @Valid NameRequest request) {
        return courses.updateCategory(id, request.name());
    }

    @DeleteMapping("/categories/{id}")
    @ResponseStatus(NO_CONTENT)
    void deleteCategory(@PathVariable @Positive long id) {
        courses.deleteCategory(id);
    }

    @PostMapping("/tags")
    ResponseEntity<CatalogEntry> createTag(@RequestBody @Valid NameRequest request) {
        final var tag = courses.createTag(request.name());
        return ResponseEntity.created(URI.create("/api/v1/tags/" + tag.id())).body(tag);
    }

    @GetMapping("/tags/{id}")
    CatalogEntry findTag(@PathVariable @Positive long id) {
        return courses.findTagById(id);
    }

    @PutMapping("/tags/{id}")
    CatalogEntry updateTag(@PathVariable @Positive long id, @RequestBody @Valid NameRequest request) {
        return courses.updateTag(id, request.name());
    }

    @DeleteMapping("/tags/{id}")
    @ResponseStatus(NO_CONTENT)
    void deleteTag(@PathVariable @Positive long id) {
        courses.deleteTag(id);
    }
}
