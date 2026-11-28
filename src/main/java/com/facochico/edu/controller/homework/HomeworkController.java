package com.facochico.edu.controller.homework;

import com.facochico.edu.controller.dto.PageQuery;
import com.facochico.edu.controller.homework.dto.GradeRequest;
import com.facochico.edu.controller.homework.dto.SubmissionRequest;
import com.facochico.edu.domain.homework.dto.AssignmentDetails;
import com.facochico.edu.domain.homework.dto.AssignmentView;
import com.facochico.edu.domain.homework.dto.SubmissionView;
import com.facochico.edu.service.homework.HomeworkService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
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
@RequestMapping("/api/v1")
@RequiredArgsConstructor
class HomeworkController {
    private final HomeworkService homework;

    @PostMapping("/lessons/{lessonId}/assignments")
    ResponseEntity<AssignmentView> createAssignment(
        @PathVariable @Positive long lessonId,
        @RequestHeader("X-User-Id") @Positive long teacherId,
        @RequestBody AssignmentDetails details
    ) {
        final var assignment = homework.createAssignment(lessonId, teacherId, details);
        return ResponseEntity.created(URI.create("/api/v1/assignments/" + assignment.id())).body(assignment);
    }

    @GetMapping("/lessons/{lessonId}/assignments")
    List<AssignmentView> listAssignments(
        @PathVariable @Positive long lessonId,
        @RequestHeader("X-User-Id") @Positive long actorId,
        @ModelAttribute @Valid PageQuery page
    ) {
        return homework.listAssignments(lessonId, actorId, page.page(), page.size());
    }

    @GetMapping("/assignments/{id}")
    AssignmentView findAssignment(@PathVariable @Positive long id, @RequestHeader("X-User-Id") @Positive long actorId) {
        return homework.findAssignment(id, actorId);
    }

    @PutMapping("/assignments/{id}")
    AssignmentView updateAssignment(
        @PathVariable @Positive long id,
        @RequestHeader("X-User-Id") @Positive long teacherId,
        @RequestBody AssignmentDetails details
    ) {
        return homework.updateAssignment(id, teacherId, details);
    }

    @DeleteMapping("/assignments/{id}")
    @ResponseStatus(NO_CONTENT)
    void deleteAssignment(@PathVariable @Positive long id, @RequestHeader("X-User-Id") @Positive long teacherId) {
        homework.deleteAssignment(id, teacherId);
    }

    @PostMapping("/assignments/{assignmentId}/submissions")
    ResponseEntity<SubmissionView> submit(
        @PathVariable @Positive long assignmentId,
        @RequestHeader("X-User-Id") @Positive long studentId,
        @RequestBody @Valid SubmissionRequest request
    ) {
        final var submission = homework.submit(assignmentId, studentId, request.content());
        return ResponseEntity.created(URI.create("/api/v1/submissions/" + submission.id())).body(submission);
    }

    @GetMapping("/assignments/{assignmentId}/submissions")
    List<SubmissionView> listSubmissionsByAssignment(
        @PathVariable @Positive long assignmentId,
        @RequestHeader("X-User-Id") @Positive long teacherId,
        @ModelAttribute @Valid PageQuery page
    ) {
        return homework.listSubmissionsByAssignment(assignmentId, teacherId, page.page(), page.size());
    }

    @GetMapping("/submissions")
    List<SubmissionView> listSubmissionsByStudent(
        @RequestHeader("X-User-Id") @Positive long studentId,
        @ModelAttribute @Valid PageQuery page
    ) {
        return homework.listSubmissionsByStudent(studentId, page.page(), page.size());
    }

    @GetMapping("/submissions/{id}")
    SubmissionView findSubmission(@PathVariable @Positive long id, @RequestHeader("X-User-Id") @Positive long actorId) {
        return homework.findSubmission(id, actorId);
    }

    @PutMapping("/submissions/{id}/grade")
    SubmissionView grade(
        @PathVariable @Positive long id,
        @RequestHeader("X-User-Id") @Positive long teacherId,
        @RequestBody @Valid GradeRequest request
    ) {
        return homework.gradeSubmission(id, teacherId, request.score(), request.feedback());
    }
}
