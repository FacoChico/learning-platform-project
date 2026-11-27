package com.facochico.edu.util.persistence;

import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;

import lombok.experimental.UtilityClass;

@UtilityClass
public class ConstraintViolationUtil {

    public boolean violatesConstraint(DataIntegrityViolationException exception, String constraint) {
        return exception.getCause() instanceof ConstraintViolationException violation
            && constraint.equals(violation.getConstraintName());
    }
}
