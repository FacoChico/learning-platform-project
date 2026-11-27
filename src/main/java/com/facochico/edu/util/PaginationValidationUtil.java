package com.facochico.edu.util;

import lombok.experimental.UtilityClass;

@UtilityClass
public class PaginationValidationUtil {

    public void requirePage(int page, int size) {
        if (page < 0 || size < 1) {
            throw new IllegalArgumentException("Page must be non-negative and page size must be positive");
        }
    }
}
