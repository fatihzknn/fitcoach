package com.fitcoach.auth.dto;

import com.fitcoach.auth.Role;
import com.fitcoach.auth.User;

import java.util.UUID;

public record UserDto(UUID id, String email, String displayName, Role role) {
    public static UserDto from(User user) {
        return new UserDto(user.getId(), user.getEmail(), user.getDisplayName(), user.getRole());
    }
}
