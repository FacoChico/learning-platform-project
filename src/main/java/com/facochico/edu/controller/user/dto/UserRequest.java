package com.facochico.edu.controller.user.dto;

import com.facochico.edu.domain.user.Profile;
import com.facochico.edu.domain.user.UserDetails;
import com.facochico.edu.domain.user.UserRole;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record UserRequest(@NotBlank String name, @NotBlank String email, @NotNull UserRole role, ProfileRequest profile) {

    public UserDetails toUserDetails() {
        final var userProfile = profile == null
            ? null
            : new Profile(profile.bio(), profile.avatarUrl());
        return new UserDetails(name, email, role, userProfile);
    }

    public record ProfileRequest(String bio, String avatarUrl) {
    }
}
