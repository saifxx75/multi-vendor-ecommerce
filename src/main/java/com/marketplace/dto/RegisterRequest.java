package com.marketplace.dto;

import com.marketplace.entity.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank @Size(max = 100) String name,
        @NotBlank @Email @Size(max = 150) String email,
        @NotBlank @Size(min = 8, max = 100, message = "must be between 8 and 100 characters") String password,
        @NotNull(message = "role is required (CUSTOMER or VENDOR)") Role role,
        @Size(max = 150) String storeName,
        @Size(max = 1000) String storeDescription) {
}
