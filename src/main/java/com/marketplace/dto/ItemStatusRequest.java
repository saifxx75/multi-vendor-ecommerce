package com.marketplace.dto;

import com.marketplace.entity.ItemStatus;
import jakarta.validation.constraints.NotNull;

public record ItemStatusRequest(@NotNull ItemStatus status) {
}
