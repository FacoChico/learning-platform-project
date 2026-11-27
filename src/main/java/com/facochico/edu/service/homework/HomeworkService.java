package com.facochico.edu.service.homework;

import com.facochico.edu.domain.homework.dto.AssignmentDetails;
import com.facochico.edu.domain.homework.dto.AssignmentView;
import com.facochico.edu.domain.homework.dto.SubmissionView;
import com.facochico.edu.repository.homework.HomeworkRepository;
import com.facochico.edu.util.PaginationValidationUtil;
import com.facochico.edu.util.homework.HomeworkValidationUtil;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

import lombok.RequiredArgsConstructor;

import static java.time.temporal.ChronoUnit.MICROS;

@RequiredArgsConstructor
public class HomeworkService {
    private final HomeworkRepository homework;
    private final Clock clock;

    public AssignmentView createAssignment(long lessonId, long teacherId, AssignmentDetails details) {
        return homework.insertAssignment(lessonId, teacherId, details);
    }

    public AssignmentView findAssignment(long id, long actorId) {
        return homework.findAssignment(id, actorId);
    }

    public List<AssignmentView> listAssignments(long lessonId, long actorId, int page, int size) {
        PaginationValidationUtil.requirePage(page, size);
        return homework.listAssignments(lessonId, actorId, page, size);
    }

    public AssignmentView updateAssignment(long id, long teacherId, AssignmentDetails details) {
        return homework.updateAssignment(id, teacherId, details);
    }

    public void deleteAssignment(long id, long teacherId) {
        homework.deleteAssignment(id, teacherId);
    }

    public SubmissionView submit(long assignmentId, long studentId, String content) {
        HomeworkValidationUtil.requireSubmissionContent(content);
        final var submittedAt = Instant.now(clock).truncatedTo(MICROS);
        return homework.insertSubmission(assignmentId, studentId, content, submittedAt);
    }

    public SubmissionView findSubmission(long id, long actorId) {
        return homework.findSubmission(id, actorId);
    }

    public SubmissionView gradeSubmission(long id, long teacherId, int score, String feedback) {
        return homework.gradeSubmission(id, teacherId, score, feedback);
    }

    public List<SubmissionView> listSubmissionsByAssignment(long assignmentId, long teacherId, int page, int size) {
        PaginationValidationUtil.requirePage(page, size);
        return homework.listSubmissionsByAssignment(assignmentId, teacherId, page, size);
    }

    public List<SubmissionView> listSubmissionsByStudent(long studentId, int page, int size) {
        PaginationValidationUtil.requirePage(page, size);
        return homework.listSubmissionsByStudent(studentId, page, size);
    }
}
