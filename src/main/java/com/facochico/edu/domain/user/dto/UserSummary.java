package com.facochico.edu.domain.user.dto;

import com.facochico.edu.domain.user.UserRole;

public record UserSummary(Long id, String name, UserRole role) {
}
