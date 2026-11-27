package com.facochico.edu.user;

import java.util.Locale;
import java.util.regex.Pattern;

public record UserDetails(String name, String email, UserRole role, Profile profile) {
    public static final int MAX_NAME_LENGTH = 255;
    public static final int MAX_EMAIL_LENGTH = 254;
    private static final Pattern EMAIL_PATTERN = Pattern.compile("[^\\s@]+@[^\\s@]+");

    public UserDetails {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("User name is required");
        }
        name = name.strip();
        if (name.length() > MAX_NAME_LENGTH) {
            throw new IllegalArgumentException("User name must be at most " + MAX_NAME_LENGTH + " characters");
        }

        if (email == null) {
            throw new IllegalArgumentException("User email is required");
        }
        email = email.strip().toLowerCase(Locale.ROOT);
        if (email.length() > MAX_EMAIL_LENGTH || !EMAIL_PATTERN.matcher(email).matches()) {
            throw new IllegalArgumentException("User email is invalid");
        }

        if (role == null) {
            throw new IllegalArgumentException("User role is required");
        }
    }
}
