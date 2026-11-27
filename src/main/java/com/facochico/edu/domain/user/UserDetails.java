package com.facochico.edu.domain.user;

import com.facochico.edu.util.user.UserValidationUtil;

import lombok.Value;

@Value
public class UserDetails {
    private final String name;
    private final String email;
    private final UserRole role;
    private final Profile profile;

    public UserDetails(String name, String email, UserRole role, Profile profile) {
        this.name = UserValidationUtil.normalizeName(name);
        this.email = UserValidationUtil.normalizeEmail(email);
        this.role = UserValidationUtil.requirePresent(role, "User role is required");
        this.profile = profile;
    }
}
