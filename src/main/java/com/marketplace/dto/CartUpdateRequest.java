package com.marketplace.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record CartUpdateRequest(
        @NotNull @Min(value = 1, message = "must be at least 1") Integer quantity) {
}
