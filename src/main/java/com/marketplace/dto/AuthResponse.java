package com.marketplace.dto;

import com.marketplace.entity.Role;

public record AuthResponse(
        String token,
        String tokenType,
        Long userId,
        String name,
        String email,
        Role role) {
}
