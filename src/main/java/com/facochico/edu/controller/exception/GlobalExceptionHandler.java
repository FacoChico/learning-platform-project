package com.facochico.edu.controller.exception;

import com.facochico.edu.domain.course.exception.CatalogEntryInUseException;
import com.facochico.edu.domain.course.exception.CourseInUseException;
import com.facochico.edu.domain.course.exception.CourseReviewAccessDeniedException;
import com.facochico.edu.domain.course.exception.CourseReviewNotFoundException;
import com.facochico.edu.domain.course.exception.CurriculumItemNotFoundException;
import com.facochico.edu.domain.course.exception.InvalidCourseTeacherException;
import com.facochico.edu.domain.enrollment.exception.DuplicateEnrollmentException;
import com.facochico.edu.domain.enrollment.exception.EnrollmentNotFoundException;
import com.facochico.edu.domain.homework.exception.DuplicateSubmissionException;
import com.facochico.edu.domain.homework.exception.HomeworkAccessDeniedException;
import com.facochico.edu.domain.homework.exception.HomeworkInUseException;
import com.facochico.edu.domain.homework.exception.HomeworkNotFoundException;
import com.facochico.edu.domain.homework.exception.InvalidScoreException;
import com.facochico.edu.domain.homework.exception.SubmissionDeadlinePassedException;
import com.facochico.edu.domain.quiz.exception.DuplicateQuizException;
import com.facochico.edu.domain.quiz.exception.InvalidQuizAnswersException;
import com.facochico.edu.domain.quiz.exception.QuizAccessDeniedException;
import com.facochico.edu.domain.quiz.exception.QuizInUseException;
import com.facochico.edu.domain.quiz.exception.QuizNotFoundException;
import com.facochico.edu.domain.user.exception.EmailAlreadyInUseException;
import com.facochico.edu.domain.user.exception.InvalidStudentRoleException;
import com.facochico.edu.domain.user.exception.UserNotFoundException;

import org.jspecify.annotations.Nullable;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.Objects;

import lombok.NonNull;

import tools.jackson.databind.exc.ValueInstantiationException;

import static java.util.stream.Collectors.toMap;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@RestControllerAdvice
class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler({
        IllegalArgumentException.class,
        InvalidCourseTeacherException.class,
        InvalidScoreException.class,
        InvalidQuizAnswersException.class
    })
    ProblemDetail invalidRequest(RuntimeException exception) {
        return ProblemDetail.forStatusAndDetail(BAD_REQUEST, exception.getMessage());
    }

    @ExceptionHandler({
        InvalidStudentRoleException.class,
        CourseReviewAccessDeniedException.class,
        HomeworkAccessDeniedException.class,
        QuizAccessDeniedException.class
    })
    ProblemDetail accessDenied(RuntimeException exception) {
        return ProblemDetail.forStatusAndDetail(FORBIDDEN, exception.getMessage());
    }

    @ExceptionHandler({
        UserNotFoundException.class,
        CurriculumItemNotFoundException.class,
        EnrollmentNotFoundException.class,
        CourseReviewNotFoundException.class,
        HomeworkNotFoundException.class,
        QuizNotFoundException.class
    })
    ProblemDetail notFound(RuntimeException exception) {
        return ProblemDetail.forStatusAndDetail(NOT_FOUND, exception.getMessage());
    }

    @ExceptionHandler({
        EmailAlreadyInUseException.class,
        CatalogEntryInUseException.class,
        CourseInUseException.class,
        DuplicateEnrollmentException.class,
        DuplicateSubmissionException.class,
        DuplicateQuizException.class,
        HomeworkInUseException.class,
        QuizInUseException.class,
        SubmissionDeadlinePassedException.class
    })
    ProblemDetail conflict(RuntimeException exception) {
        return ProblemDetail.forStatusAndDetail(CONFLICT, exception.getMessage());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ProblemDetail dataConflict() {
        return ProblemDetail.forStatusAndDetail(CONFLICT, "The change conflicts with existing data");
    }

    @Override
    protected @Nullable ResponseEntity<Object> handleMethodArgumentNotValid(
        @NonNull MethodArgumentNotValidException exception,
        @NonNull HttpHeaders headers,
        @NonNull HttpStatusCode status,
        @NonNull WebRequest request
    ) {
        final var errors = exception.getBindingResult().getFieldErrors().stream()
            .collect(toMap(FieldError::getField, error -> Objects.toString(error.getDefaultMessage(), "Invalid value"),
                (first, ignored) -> first));
        final var problem = ProblemDetail.forStatusAndDetail(status, "Request validation failed");
        problem.setProperty("errors", errors);

        return handleExceptionInternal(exception, problem, headers, status, request);
    }

    @Override
    protected @Nullable ResponseEntity<Object> handleHttpMessageNotReadable(
        @NonNull HttpMessageNotReadableException exception,
        @NonNull HttpHeaders headers,
        @NonNull HttpStatusCode status,
        @NonNull WebRequest request
    ) {
        final var problem = ProblemDetail.forStatusAndDetail(status, bodyErrorDetail(exception));
        return handleExceptionInternal(exception, problem, headers, status, request);
    }

    private static String bodyErrorDetail(HttpMessageNotReadableException exception) {
        if (exception.getCause() instanceof ValueInstantiationException failure
            && failure.getType().getRawClass().getPackageName().startsWith("com.facochico.edu.domain.")
            && failure.getCause() instanceof IllegalArgumentException invalidDetails) {
            return invalidDetails.getMessage();
        }
        return "Request body must contain valid JSON with the expected fields";
    }
}
