package com.marketplace.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CheckoutRequest(
        @NotBlank @Size(max = 500) String shippingAddress) {
}
