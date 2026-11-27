package com.facochico.edu.repository.homework;

import com.facochico.edu.domain.course.CourseEntity;
import com.facochico.edu.domain.course.LessonEntity;
import com.facochico.edu.domain.course.exception.CurriculumItemNotFoundException;
import com.facochico.edu.domain.homework.AssignmentEntity;
import com.facochico.edu.domain.homework.SubmissionEntity;
import com.facochico.edu.domain.homework.dto.AssignmentDetails;
import com.facochico.edu.domain.homework.dto.AssignmentView;
import com.facochico.edu.domain.homework.dto.SubmissionView;
import com.facochico.edu.domain.homework.exception.DuplicateSubmissionException;
import com.facochico.edu.domain.homework.exception.HomeworkAccessDeniedException;
import com.facochico.edu.domain.homework.exception.HomeworkInUseException;
import com.facochico.edu.domain.homework.exception.HomeworkNotFoundException;
import com.facochico.edu.domain.user.UserEntity;
import com.facochico.edu.domain.user.exception.UserNotFoundException;
import com.facochico.edu.repository.course.EnrollmentRepository;
import com.facochico.edu.util.persistence.ConstraintViolationUtil;
import com.facochico.edu.util.user.UserValidationUtil;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

import jakarta.persistence.EntityManager;

import lombok.RequiredArgsConstructor;

import static com.facochico.edu.domain.homework.SubmissionEntity.ASSIGNMENT_FOREIGN_KEY;
import static com.facochico.edu.domain.homework.SubmissionEntity.ASSIGNMENT_STUDENT_UNIQUE;
import static com.facochico.edu.domain.user.UserRole.STUDENT;
import static com.facochico.edu.domain.user.UserRole.TEACHER;
import static lombok.AccessLevel.PACKAGE;

@Repository
@Transactional
@RequiredArgsConstructor(access = PACKAGE)
class JpaHomeworkRepository implements HomeworkRepository {
    private final SpringDataAssignmentRepository assignments;
    private final SpringDataSubmissionRepository submissions;
    private final EnrollmentRepository enrollments;
    private final EntityManager entityManager;

    @Override
    public AssignmentView insertAssignment(long lessonId, long teacherId, AssignmentDetails details) {
        final var lesson = requireLesson(lessonId);
        requireCourseTeacher(courseOf(lesson), teacherId);

        final var assignment = assignments.save(new AssignmentEntity(lesson, details));
        return toAssignmentView(assignment);
    }

    @Override
    @Transactional(readOnly = true)
    public AssignmentView findAssignment(long id, long actorId) {
        final var assignment = requireAssignment(id);
        requireAssignmentAccess(courseOf(assignment.getLesson()), actorId);

        return toAssignmentView(assignment);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AssignmentView> listAssignments(long lessonId, long actorId, int page, int size) {
        final var lesson = requireLesson(lessonId);
        requireAssignmentAccess(courseOf(lesson), actorId);

        return assignments.findByLessonIdOrderById(lessonId, PageRequest.of(page, size)).stream()
            .map(JpaHomeworkRepository::toAssignmentView)
            .toList();
    }

    @Override
    public AssignmentView updateAssignment(long id, long teacherId, AssignmentDetails details) {
        final var assignment = requireAssignmentForUpdate(id);
        requireCourseTeacher(courseOf(assignment.getLesson()), teacherId);

        final var highestScore = submissions.findHighestScore(id).orElse(0);
        assignment.update(details, highestScore);
        return toAssignmentView(assignment);
    }

    @Override
    public void deleteAssignment(long id, long teacherId) {
        final var assignment = requireAssignmentForUpdate(id);
        requireCourseTeacher(courseOf(assignment.getLesson()), teacherId);

        try {
            assignments.delete(assignment);
            assignments.flush();
        } catch (DataIntegrityViolationException exception) {
            if (ConstraintViolationUtil.violatesConstraint(exception, ASSIGNMENT_FOREIGN_KEY)) {
                throw new HomeworkInUseException("Assignment", id, exception);
            }
            throw exception;
        }
    }

    @Override
    public SubmissionView insertSubmission(long assignmentId, long studentId, String content, Instant submittedAt) {
        final var assignment = requireAssignmentForUpdate(assignmentId);
        final var student = requireUser(studentId);
        UserValidationUtil.requireStudent(student.getRole());
        requireEnrollment(courseOf(assignment.getLesson()).getId(), studentId);

        final var submission = new SubmissionEntity(assignment, student, submittedAt, content);
        try {
            return toSubmissionView(submissions.saveAndFlush(submission));
        } catch (DataIntegrityViolationException exception) {
            if (ConstraintViolationUtil.violatesConstraint(exception, ASSIGNMENT_STUDENT_UNIQUE)) {
                throw new DuplicateSubmissionException(assignmentId, studentId, exception);
            }
            throw exception;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public SubmissionView findSubmission(long id, long actorId) {
        final var submission = requireSubmission(id);
        final var actor = requireUser(actorId);
        final var isSubmittingStudent = actor.getRole() == STUDENT
            && submission.getStudent().getId().equals(actor.getId());
        if (!isSubmittingStudent && !isCourseTeacher(courseOf(submission.getAssignment().getLesson()), actor)) {
            throw new HomeworkAccessDeniedException("Only the submitting student or course teacher may access the submission");
        }
        return toSubmissionView(submission);
    }

    @Override
    public SubmissionView gradeSubmission(long id, long teacherId, int score, String feedback) {
        final var assignmentId = submissions.findAssignmentId(id)
            .orElseThrow(() -> new HomeworkNotFoundException("Submission", id));
        final var assignment = requireAssignmentForUpdate(assignmentId);
        requireCourseTeacher(courseOf(assignment.getLesson()), teacherId);

        final var submission = requireSubmission(id);
        submission.grade(score, feedback);
        return toSubmissionView(submission);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SubmissionView> listSubmissionsByAssignment(long assignmentId, long teacherId, int page, int size) {
        final var assignment = requireAssignment(assignmentId);
        requireCourseTeacher(courseOf(assignment.getLesson()), teacherId);

        return submissions.findByAssignmentIdOrderById(assignmentId, PageRequest.of(page, size)).stream()
            .map(JpaHomeworkRepository::toSubmissionView)
            .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<SubmissionView> listSubmissionsByStudent(long studentId, int page, int size) {
        final var student = requireUser(studentId);
        UserValidationUtil.requireStudent(student.getRole());

        return submissions.findByStudentIdOrderById(studentId, PageRequest.of(page, size)).stream()
            .map(JpaHomeworkRepository::toSubmissionView)
            .toList();
    }

    private LessonEntity requireLesson(long lessonId) {
        final var lesson = entityManager.find(LessonEntity.class, lessonId);
        if (lesson == null) {
            throw new CurriculumItemNotFoundException("Lesson", lessonId);
        }
        return lesson;
    }

    private AssignmentEntity requireAssignment(long id) {
        return assignments.findById(id)
            .orElseThrow(() -> new HomeworkNotFoundException("Assignment", id));
    }

    private AssignmentEntity requireAssignmentForUpdate(long id) {
        return assignments.findForUpdate(id)
            .orElseThrow(() -> new HomeworkNotFoundException("Assignment", id));
    }

    private SubmissionEntity requireSubmission(long id) {
        return submissions.findById(id)
            .orElseThrow(() -> new HomeworkNotFoundException("Submission", id));
    }

    private UserEntity requireUser(long id) {
        final var user = entityManager.find(UserEntity.class, id);
        if (user == null) {
            throw new UserNotFoundException(id);
        }
        return user;
    }

    private void requireCourseTeacher(CourseEntity course, long teacherId) {
        if (!isCourseTeacher(course, requireUser(teacherId))) {
            throw new HomeworkAccessDeniedException("Only the course teacher may manage homework");
        }
    }

    private void requireAssignmentAccess(CourseEntity course, long actorId) {
        final var actor = requireUser(actorId);
        final var isTeacher = isCourseTeacher(course, actor);
        final var isEnrolledStudent = actor.getRole() == STUDENT
            && enrollments.findEnrollment(course.getId(), actorId).isPresent();

        if (!isTeacher && !isEnrolledStudent) {
            throw new HomeworkAccessDeniedException("Only the course teacher or an enrolled student may access assignments");
        }
    }

    private void requireEnrollment(long courseId, long studentId) {
        if (enrollments.findEnrollment(courseId, studentId).isEmpty()) {
            throw new HomeworkAccessDeniedException("Only enrolled students may access assignments");
        }
    }

    private static boolean isCourseTeacher(CourseEntity course, UserEntity actor) {
        return actor.getRole() == TEACHER && course.getTeacher().getId().equals(actor.getId());
    }

    private static CourseEntity courseOf(LessonEntity lesson) {
        return lesson.getModule().getCourse();
    }

    private static AssignmentView toAssignmentView(AssignmentEntity assignment) {
        final var details = new AssignmentDetails(
            assignment.getTitle(), assignment.getDescription(), assignment.getDueAt(), assignment.getMaxScore()
        );
        return new AssignmentView(assignment.getId(), assignment.getLesson().getId(), details);
    }

    private static SubmissionView toSubmissionView(SubmissionEntity submission) {
        return new SubmissionView(
            submission.getId(), submission.getAssignment().getId(), submission.getStudent().getId(),
            submission.getSubmittedAt(), submission.getContent(), submission.getStatus(), submission.getScore(),
            submission.getFeedback()
        );
    }
}
