package com.facochico.edu.repository.homework;

import com.facochico.edu.domain.homework.dto.AssignmentDetails;
import com.facochico.edu.domain.homework.dto.AssignmentView;
import com.facochico.edu.domain.homework.dto.SubmissionView;

import java.time.Instant;
import java.util.List;

public interface HomeworkRepository {

    AssignmentView insertAssignment(long lessonId, long teacherId, AssignmentDetails details);

    AssignmentView findAssignment(long id, long actorId);

    List<AssignmentView> listAssignments(long lessonId, long actorId, int page, int size);

    AssignmentView updateAssignment(long id, long teacherId, AssignmentDetails details);

    void deleteAssignment(long id, long teacherId);

    SubmissionView insertSubmission(long assignmentId, long studentId, String content, Instant submittedAt);

    SubmissionView findSubmission(long id, long actorId);

    SubmissionView gradeSubmission(long id, long teacherId, int score, String feedback);

    List<SubmissionView> listSubmissionsByAssignment(long assignmentId, long teacherId, int page, int size);

    List<SubmissionView> listSubmissionsByStudent(long studentId, int page, int size);
}
