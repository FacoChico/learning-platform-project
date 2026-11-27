package com.facochico.edu.domain.course.exception;

public final class InvalidCourseTeacherException extends RuntimeException {

    public InvalidCourseTeacherException() {
        super("Course teacher must have the TEACHER role");
    }
}
