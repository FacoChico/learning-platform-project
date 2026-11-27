package com.facochico.edu.util.course;

import com.facochico.edu.domain.course.exception.InvalidCourseTeacherException;
import com.facochico.edu.domain.user.UserRole;

import java.util.Set;

import lombok.experimental.UtilityClass;

import static com.facochico.edu.domain.user.UserRole.TEACHER;

@UtilityClass
public class CurriculumValidationUtil {
    private static final int MAX_LABEL_LENGTH = 255;

    public String requireLabel(String value, String label) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(label + " is required");
        }

        final var normalized = value.strip();
        if (normalized.length() > MAX_LABEL_LENGTH) {
            throw new IllegalArgumentException(label + " must be at most " + MAX_LABEL_LENGTH + " characters");
        }
        return normalized;
    }

    public void requirePosition(int orderIndex) {
        if (orderIndex < 0) {
            throw new IllegalArgumentException("Order index must be non-negative");
        }
    }

    public void requireId(long id, String resource) {
        if (id < 1) {
            throw new IllegalArgumentException(resource + " ID must be positive");
        }
    }

    public Set<Long> copyTagIds(Set<Long> tagIds) {
        if (tagIds == null) {
            return Set.of();
        }

        for (final var id : tagIds) {
            if (id == null || id < 1) {
                throw new IllegalArgumentException("Tag ID must be positive");
            }
        }
        return Set.copyOf(tagIds);
    }

    public void requireTeacher(UserRole role) {
        if (role != TEACHER) {
            throw new InvalidCourseTeacherException();
        }
    }
}
