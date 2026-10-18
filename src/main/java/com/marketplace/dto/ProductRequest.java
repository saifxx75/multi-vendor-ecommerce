package com.marketplace.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record ProductRequest(
        @NotBlank @Size(max = 200) String name,
        @Size(max = 2000) String description,
        @NotNull @DecimalMin(value = "0.01", message = "must be at least 0.01")
        @Digits(integer = 10, fraction = 2, message = "must have at most 10 integer and 2 decimal digits") BigDecimal price,
        @NotNull @Min(value = 0, message = "must not be negative") Integer stock,
        @NotNull Long categoryId) {
}
