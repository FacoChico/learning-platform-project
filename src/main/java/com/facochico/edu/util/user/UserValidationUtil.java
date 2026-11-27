package com.facochico.edu.util.user;

import java.util.Locale;
import java.util.regex.Pattern;

import lombok.experimental.UtilityClass;

@UtilityClass
public class UserValidationUtil {
    public static final int MAX_NAME_LENGTH = 255;
    public static final int MAX_EMAIL_LENGTH = 254;
    private static final Pattern EMAIL_PATTERN = Pattern.compile("[^\\s@]+@[^\\s@]+");

    public String normalizeName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("User name is required");
        }

        final var normalizedName = name.strip();
        if (normalizedName.length() > MAX_NAME_LENGTH) {
            throw new IllegalArgumentException("User name must be at most " + MAX_NAME_LENGTH + " characters");
        }
        return normalizedName;
    }

    public String normalizeEmail(String email) {
        final var normalizedEmail = requirePresent(email, "User email is required").strip().toLowerCase(Locale.ROOT);
        if (normalizedEmail.length() > MAX_EMAIL_LENGTH || !EMAIL_PATTERN.matcher(normalizedEmail).matches()) {
            throw new IllegalArgumentException("User email is invalid");
        }
        return normalizedEmail;
    }

    public <T> T requirePresent(T value, String message) {
        if (value == null) {
            throw new IllegalArgumentException(message);
        }
        return value;
    }
}
